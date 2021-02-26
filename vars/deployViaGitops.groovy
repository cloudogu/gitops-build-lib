#!groovy
import com.cloudogu.gitopsbuildlib.*

String getConfigDir() { '.config' }

List getMandatoryFields() {
    return [
        'scmmCredentialsId', 'scmmConfigRepoUrl', 'scmmPullRequestBaseUrl', 'scmmPullRequestRepo', 'application', 'stages'
    ]
}

Map getDefaultConfig() {
    String helmImage = 'ghcr.io/cloudogu/helm:3.4.1-1'

    return [
        cesBuildLibRepo   : 'https://github.com/cloudogu/ces-build-lib',
        cesBuildLibVersion: '1.45.0',
        mainBranch        : 'main',
        deployments       : [
            sourcePath: 'k8s',
        ],
        validators        : [
            kubeval : [
                validator: new Kubeval(this),
                enabled  : true,
                config   : [
                    // We use the helm image (that also contains kubeval plugin) to speed up builds by allowing to reuse image
                    image           : helmImage,
                    k8sSchemaVersion: '1.18.1'
                ]
            ],
            yamllint: [
                validator: new Yamllint(this),
                enabled  : true,
                config   : [
                    image  : 'cytopia/yamllint:1.25-0.7',
                    // Default to relaxed profile because it's feasible for mere mortalYAML programmers.
                    // It still fails on syntax errors.
                    profile: 'relaxed'
                ]
            ]
        ]
    ]
}

void call(Map gitopsConfig) {
    // Merge default config with the one passed as parameter
    gitopsConfig = mergeMaps(defaultConfig, gitopsConfig)
    validateConfig(gitopsConfig)
    cesBuildLib = initCesBuildLib(gitopsConfig.cesBuildLibRepo, gitopsConfig.cesBuildLibVersion)
    deploy(gitopsConfig)
}

def mergeMaps(Map a, Map b) {
    return b.inject(a.clone()) { map, entry ->
        if (map[entry.key] instanceof Map && entry.value instanceof Map) {
            map[entry.key] = mergeMaps(map[entry.key], entry.value)
        } else {
            map[entry.key] = entry.value
        }
        return map
    }
}

def validateConfig(Map gitopsConfig) {
    validateMandatoryFields(gitopsConfig)
    validateDeploymentConfig(gitopsConfig.deployments)
}

def validateMandatoryFields(Map gitopsConfig) {
    def nonValidFields = []
    for (String mandatoryField : mandatoryFields) {
        // Note: "[]" syntax (and also getProperty()) leads to
        // Scripts not permitted to use staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods getAt
        if (!gitopsConfig.containsKey(mandatoryField)) {
            nonValidFields += mandatoryField
        } else {
            def mandatoryFieldValue = gitopsConfig.get(mandatoryField)
            if (!mandatoryFieldValue) {
                nonValidFields += mandatoryField
            }
        }
    }
    if (nonValidFields) {
        error 'The following fields in the gitops config are mandatory but were not set or have invalid values: ' + nonValidFields
    }
}

def validateDeploymentConfig(Map deployments) {
    if (deployments.containsKey('plain') && deployments.containsKey('helm')) {
        error 'Please choose between \'deployments.plain\' and \'deployments.helm\'. Setting both properties is not possible!'
    }
    if (!deployments.containsKey('plain') && !deployments.containsKey('helm')) {
        error 'One of \'deployments.plain\' or \'deployments.helm\' must be set!'
    }
}

protected initCesBuildLib(cesBuildLibRepo, cesBuildLibVersion) {
    return library(identifier: "ces-build-lib@${cesBuildLibVersion}",
        retriever: modernSCM([$class: 'GitSCMSource', remote: cesBuildLibRepo])
    ).com.cloudogu.ces.cesbuildlib
}

protected void deploy(Map gitopsConfig) {
    def git = cesBuildLib.Git.new(this, gitopsConfig.scmmCredentialsId)
    def gitRepo = prepareGitRepo(git)
    def changesOnGitOpsRepo = ''

    try {
        dir(gitRepo.configRepoTempDir) {

            git url: gitopsConfig.scmmConfigRepoUrl, branch: gitopsConfig.mainBranch, changelog: false, poll: false
            git.fetch()

            changesOnGitOpsRepo = aggregateChangesOnGitOpsRepo(syncGitopsRepoPerStage(gitopsConfig, git, gitRepo))
        }
    } finally {
        sh "rm -rf ${gitRepo.configRepoTempDir}"
    }

    //TODO check if plain or helm
    currentBuild.description = createBuildDescription(changesOnGitOpsRepo, gitopsConfig.deployments.plain.updateImages.imageName as String)
}

protected Map prepareGitRepo(def git) {
    // Query and store info about application repo before cloning into gitops repo
    GitRepo applicationRepo = GitRepo.create(git)

    // Display that Jenkins made the GitOps commits, not the application repo author
    git.committerName = 'Jenkins'
    git.committerEmail = 'jenkins@cloudogu.com'

    def configRepoTempDir = '.configRepoTempDir'

    return [
        applicationRepo  : applicationRepo,
        configRepoTempDir: configRepoTempDir
    ]
}

protected HashSet<String> syncGitopsRepoPerStage(Map gitopsConfig, def git, Map gitRepo) {

    HashSet<String> allRepoChanges = new HashSet<String>()
    def scmm = cesBuildLib.SCMManager.new(this, gitopsConfig.scmmPullRequestBaseUrl, gitopsConfig.scmmCredentialsId)

    gitopsConfig.stages.each { stage, config ->
        //checkout the main_branch before creating a new stage_branch. so it won't be branched off of an already checked out stage_branch
        git.checkoutOrCreate(gitopsConfig.mainBranch)
        if (config.deployDirectly) {
            allRepoChanges += syncGitopsRepo(stage, gitopsConfig.mainBranch, git, gitRepo, gitopsConfig)
        } else {
            String stageBranch = "${stage}_${gitopsConfig.application}"
            git.checkoutOrCreate(stageBranch)
            String repoChanges = syncGitopsRepo(stage, stageBranch, git, gitRepo, gitopsConfig)

            if (repoChanges) {
                def title = 'created by service \'' + gitopsConfig.application + '\' for stage \'' + stage + '\''
                //TODO description functionality needs to be implemented
                def description = ''
                scmm.createOrUpdatePullRequest(gitopsConfig.scmmPullRequestRepo, stageBranch, gitopsConfig.mainBranch, title, description)
                allRepoChanges += repoChanges
            }
        }
    }
    return allRepoChanges
}

protected String syncGitopsRepo(String stage, String branch, def git, Map gitRepo, Map gitopsConfig) {

    // TODO this is only for k8s?
    if (gitopsConfig.deployments.containsKey('plain')) {
        createApplicationFoldersPlain(stage, gitopsConfig)
    } else if (gitopsConfig.deployments.containsKey('helm')) {
        createApplicationFoldersHelm(stage, gitopsConfig)
    }

    gitopsConfig.validators.each { validatorConfig ->
        echo "Executing validator ${validatorConfig.key}"

        // TODO pass gitopsConfig.deployments as param
        validatorConfig.value.validator.validate(
            validatorConfig.value.enabled,
            "${stage}/${gitopsConfig.application}/",
            validatorConfig.value.config)
    }
    if (gitopsConfig.deployments.containsKey('plain')) {

        // TODO move this to a PlainDeployment class and introduce a HelmDeployment class?
        gitopsConfig.deployments.plain.updateImages.each {
            updateImageVersionPlain("${stage}/${gitopsConfig.application}/${it['deploymentFilename']}", it['containerName'], it['imageName'])
        }
    } else if (gitopsConfig.deployments.containsKey('helm')) {
        updateImageVersionHelm(gitopsConfig, stage)
    }

    return commitAndPushToStage(stage, branch, git, gitRepo)
}

private void createApplicationFoldersPlain(String stage, Map gitopsConfig) {
    def sourcePath = gitopsConfig.deployments.sourcePath
    sh "mkdir -p ${stage}/${gitopsConfig.application}/"
    sh "mkdir -p ${configDir}/"
    // copy extra resources like sealed secrets
    echo "Copying k8s payload from application repo to gitOps Repo: '${sourcePath}/${stage}/*' to '${stage}/${gitopsConfig.application}'"
    sh "cp ${env.WORKSPACE}/${sourcePath}/${stage}/* ${stage}/${gitopsConfig.application}/ || true"
    sh "cp ${env.WORKSPACE}/*.yamllint.yaml ${configDir}/ || true"
}

private void createApplicationFoldersHelm(String stage, Map gitopsConfig) {
    sh "mkdir -p ${stage}/${gitopsConfig.application}/"
}

protected String commitAndPushToStage(String stage, String branch, def git, Map gitRepo) {
    String commitPrefix = "[${stage}] "
    git.add('.')
    if (git.areChangesStagedForCommit()) {
        git.commit(commitPrefix + createApplicationCommitMessage(gitRepo.applicationRepo), gitRepo.applicationRepo.authorName, gitRepo.applicationRepo.authorEmail)

        // If some else pushes between the pull above and this push, the build will fail.
        // So we pull if push fails and try again
        git.pushAndPullOnFailure("origin ${branch}")
        return "${stage} (${git.commitHashShort})"
    } else {
        echo "No changes on gitOps repo for ${stage} (branch: ${branch}). Not committing or pushing."
        return ''
    }
}

protected String aggregateChangesOnGitOpsRepo(changes) {
    // Remove empty
    (changes - '')
    // and concat into string
        .join('; ')
}

private String createApplicationCommitMessage(GitRepo applicationRepo) {
    String issueIds = (applicationRepo.commitMessage =~ /#\d*/).collect { "${it} " }.join('')

    String[] urlSplit = applicationRepo.repositoryUrl.split('/')
    def repoNamespace = urlSplit[-2]
    def repoName = urlSplit[-1]
    String message = "${issueIds}${repoNamespace}/${repoName}@${applicationRepo.commitHash}"

    return message
}

private void updateImageVersionPlain(String deploymentFilePath, String containerName, String newImageTag) {
    def data = readYaml file: deploymentFilePath
    def containers = data.spec.template.spec.containers
    def updateContainer = containers.find { it.name == containerName }
    updateContainer.image = newImageTag
    writeYaml file: deploymentFilePath, data: data, overwrite: true
}

private void updateImageVersionHelm(Map gitopsConfig, String stage) {
    def helmConfig = gitopsConfig.deployments.helm
    def application = gitopsConfig.application
    def sourcePath = gitopsConfig.deployments.sourcePath
    // writing the merged-values.yaml via writeYaml into a file has the advantage, that it gets formatted as valid yaml
    // This makes it easier to read in and indent for the inline use in the helmRelease.
    // It enables us to reuse the `fileToInlineYaml` function, without writing a complex formatting logic.
    writeFile file: "${stage}/${application}/mergedValues.yaml", text: mergeValues(helmConfig.repoUrl, ["${env.WORKSPACE}/${sourcePath}/values-${stage}.yaml", "${env.WORKSPACE}/${sourcePath}/values-shared.yaml"] as String[])
    updateYamlValue("${stage}/${application}/mergedValues.yaml", helmConfig)

    writeFile file: "${stage}/${application}/helmRelease.yaml", text: createHelmRelease(helmConfig, "fluxv1-${stage}", "${stage}/${application}/mergedValues.yaml")

    // since the values are already inline (helmRelease.yaml) we do not need to commit them into the gitops repo
    sh "rm ${stage}/${application}/mergedValues.yaml"
}

void updateYamlValue(String yamlFilePath, Map helmConfig) {
    def data = readYaml file: yamlFilePath
    helmConfig.updateValues.each {
        String[] paths = it["fieldPath"].split("\\.")
        def _tmp = data
        paths.eachWithIndex { String p, int i ->
            def tmp = _tmp.get(p)
            if (i == paths.length - 1 && tmp != null) {
                _tmp.put(p, it["newValue"])
            }
            _tmp = tmp
        }
    }

    writeYaml file: yamlFilePath, data: data, overwrite: true
}

String createHelmRelease(Map helmConfig, String namespace, String valuesFile) {
    def values = fileToInlineYaml(valuesFile)
    return """apiVersion: helm.fluxcd.io/v1
kind: HelmRelease
metadata:
  name: ${application}
  namespace: ${namespace}
  annotations:
    fluxcd.io/automated: "false"
spec:
  releaseName: ${application}
  chart:
    git: ${helmConfig.repoUrl}
    ref: ${helmConfig.version}
    path: .
  values:
    ${values}
"""
}

String fileToInlineYaml(String fileContents) {
    String values = ""
    String indent = "    "
    def fileContent = readFile fileContents
    values += fileContent.split("\\n").join("\n" + indent)
    return values
}

String mergeValues(String chart, String[] files) {
    String merge = ""
    String _files = ""
    files.each {
        _files += "-f $it "
    }

    sh "git clone ${chart} ${env.WORKSPACE}/spring-boot-helm-chart || true"

    withHelm {
        String script = "helm values ${env.WORKSPACE}/spring-boot-helm-chart ${_files}"
        merge = sh returnStdout: true, script: script
    }

    sh "rm -rf ${env.WORKSPACE}/spring-boot-helm-chart || true"

    return merge
}

void withHelm(Closure body) {
    cesBuildLib.Docker.new(this).image(helmImage)
        .inside("${pwd().equals(env.WORKSPACE) ? '' : "-v ${env.WORKSPACE}:${env.WORKSPACE}"}") {
            body()
        }
}

protected String createBuildDescription(String pushedChanges, String imageName) {
    String description = ''

    description += "GitOps commits: "

    if (pushedChanges) {
        description += pushedChanges
    } else {
        description += 'No changes'
    }

    description += "\nImage: ${imageName}"

    return description
}

def cesBuildLib
