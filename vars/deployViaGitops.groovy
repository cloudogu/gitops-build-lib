#!groovy
import com.cloudogu.gitopsbuildlib.*
import com.cloudogu.gitopsbuildlib.deployments.Deployment
import com.cloudogu.gitopsbuildlib.deployments.Helm
import com.cloudogu.gitopsbuildlib.deployments.Plain
import com.cloudogu.gitopsbuildlib.validation.Kubeval
import com.cloudogu.gitopsbuildlib.validation.Yamllint

String getHelmImage() { 'ghcr.io/cloudogu/helm:3.4.1-1' }

List getMandatoryFields() {
    return [
        'scmmCredentialsId', 'scmmConfigRepoUrl', 'scmmPullRequestBaseUrl', 'scmmPullRequestRepo', 'application', 'stages'
    ]
}

Map getDefaultConfig() {

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
    } else if (!deployments.containsKey('plain') && !deployments.containsKey('helm')) {
        error 'One of \'deployments.plain\' or \'deployments.helm\' must be set!'
    }
    if (deployments.containsKey('plain')) {
        deployment = new Plain(this)
    } else if (deployments.containsKey('helm')) {
        deployment = new Helm(this)
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

    // TODO change to method call of deplyoment interface
    if (gitopsConfig.deployments.containsKey('plain')) {
        currentBuild.description = createBuildDescription(changesOnGitOpsRepo, gitopsConfig.deployments.plain.updateImages.imageName as String)
    } else if (gitopsConfig.deployments.containsKey('helm')) {
        // TODO change description
        currentBuild.description = createBuildDescription(changesOnGitOpsRepo, gitopsConfig.deployments.helm.version)
    }
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

    deployment.createApplicationFolders(stage, gitopsConfig)

    gitopsConfig.validators.each { validatorConfig ->
        echo "Executing validator ${validatorConfig.key}"

        // TODO pass gitopsConfig.deployments as param
        validatorConfig.value.validator.validate(
            validatorConfig.value.enabled,
            "${stage}/${gitopsConfig.application}/",
            validatorConfig.value.config)
    }

    deployment.update(stage, gitopsConfig)

    return commitAndPushToStage(stage, branch, git, gitRepo)
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

// TODO put also into depoyment classes
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
Deployment deployment
