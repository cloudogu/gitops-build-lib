#!groovy

import com.cloudogu.gitopsbuildlib.GitRepo
import com.cloudogu.gitopsbuildlib.deployment.Deployment
import com.cloudogu.gitopsbuildlib.deployment.FolderStructureStrategy
import com.cloudogu.gitopsbuildlib.deployment.GitopsTool
import com.cloudogu.gitopsbuildlib.deployment.helm.Helm
import com.cloudogu.gitopsbuildlib.deployment.plain.Plain
import com.cloudogu.gitopsbuildlib.scm.SCMManager
import com.cloudogu.gitopsbuildlib.scm.SCMProvider
import com.cloudogu.gitopsbuildlib.validation.HelmKubeval
import com.cloudogu.gitopsbuildlib.validation.Kubeval
import com.cloudogu.gitopsbuildlib.validation.Yamllint 

List getMandatoryFields() {
    return [
        'scm.provider', 'scm.baseUrl', 'scm.repositoryUrl', 'application', 'stages', 'gitopsTool'
    ]
}

Map createDefaultConfig() {

    return [
        cesBuildLibRepo         : 'https://github.com/cloudogu/ces-build-lib',
        cesBuildLibVersion      : '1.62.0',
        cesBuildLibCredentialsId: '',
        mainBranch              : 'main',
        buildImages          : [
            helm: [
                credentialsId: '',
                image: 'ghcr.io/cloudogu/helm:3.11.1-2'
            ],
            kubectl: [
                credentialsId: '',
                image: 'lachlanevenson/k8s-kubectl:v1.24.8'
            ],
            // We use the helm image (that also contains kubeval plugin) to speed up builds by allowing to reuse image
            kubeval: [
                credentialsId: '',
                image: 'ghcr.io/cloudogu/helm:3.5.4-1'
            ],
            helmKubeval: [
                credentialsId: '',
                image: 'ghcr.io/cloudogu/helm:3.5.4-1'
            ],
            yamllint: [
                credentialsId: '',
                image: 'cytopia/yamllint:1.25-0.9'
            ]
        ],
        deployments             : [
            sourcePath: 'k8s',
            destinationRootPath: '.'
        ],
        validators              : [
            kubeval    : [
                validator: new Kubeval(this),
                enabled  : false,
                config   : [
                    // imageRef's are referencing the key in gitopsConfig.buildImages
                    imageRef        : 'kubeval',
                    k8sSchemaVersion: '1.18.1'
                ]
            ],
            helmKubeval: [
                validator: new HelmKubeval(this),
                enabled  : false,
                config   : [
                    imageRef        : 'helmKubeval',
                    k8sSchemaVersion: '1.18.1'
                ]
            ],
            yamllint   : [
                validator: new Yamllint(this),
                enabled  : true,
                config   : [
                    imageRef  : 'yamllint',
                    // Default to relaxed profile because it's feasible for mere mortalYAML programmers.
                    // It still fails on syntax errors.
                    profile: 'relaxed'
                ]
            ]
        ],
        stages                  : [
            staging   : [deployDirectly: true],
            production: [deployDirectly: false],
        ],
        folderStructureStrategy: 'GLOBAL_ENV'
    ]
}

void call(Map gitopsConfig) {
    // Merge default config with the one passed as parameter
    gitopsConfig = mergeMaps(createDefaultConfig(), gitopsConfig)
    if (validateConfig(gitopsConfig)) {
        cesBuildLib = initCesBuildLib(gitopsConfig.cesBuildLibRepo, gitopsConfig.cesBuildLibVersion, gitopsConfig.cesBuildLibCredentialsId)
        deploy(gitopsConfig)
    }
}

def mergeMaps(Map a, Map b) {
    return b.inject(a.clone()) { map, entry ->
        if (map[entry.key] instanceof Map && entry.value instanceof Map) {

            // due to the stages being the definition of the environment its not a merge but overwriting
            if (entry.key == 'stages')
                map[entry.key] = entry.value
            else
                map[entry.key] = mergeMaps(map[entry.key], entry.value)
        } else {
            map[entry.key] = entry.value
        }
        return map
    }
}

// Note: had to do this little hack because groovy tests should fail on invalid configurations
//       but the error class is jenkins specific so a basic boolean return value was needed.
def validateConfig(Map gitopsConfig) {
    return validateMandatoryFields(gitopsConfig) && validateDeploymentConfig(gitopsConfig)
}

// recursive call used to find keys and values that are not toplevel declarations inside the map
Map findMandatoryFieldKeyValue(def config, List<String> keys) {
    for (String key : keys) {
        if (config.containsKey(key))
            if (keys.size() == 1)
                return [first: true, second: (String) config.get(key)]
            else
                return this.findMandatoryFieldKeyValue(config.get(key), keys - key)

        else
            return [first: false, second: '']
    }
}

def validateMandatoryFields(Map gitopsConfig) {
    def nonValidFields = []
    for (String mandatoryField : mandatoryFields) {
        if (mandatoryField.contains('.')) {
            Map<Boolean, String> mandatoryFieldKeyValue = findMandatoryFieldKeyValue(gitopsConfig, mandatoryField.tokenize('.'))
            if (!mandatoryFieldKeyValue.get('first') || (mandatoryFieldKeyValue.get('first') && !mandatoryFieldKeyValue.get('second'))) {
                nonValidFields += mandatoryField
            }

            // Note: "[]" syntax (and also getProperty()) leads to
            // Scripts not permitted to use staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods getAt
        } else if (!gitopsConfig.containsKey(mandatoryField)) {
            nonValidFields += mandatoryField
        } else {
            def mandatoryFieldValue = gitopsConfig.get(mandatoryField)
            if (!mandatoryFieldValue)
                nonValidFields += mandatoryField
        }
    }

    if (nonValidFields) {
        error 'The following fields in the gitops config are mandatory but were not set or have invalid values: ' + nonValidFields
        return false
    }
    return true
}

def validateDeploymentConfig(Map gitopsConfig) {
    // choose whether to execute plain or helm deployments
    if (gitopsConfig.deployments.containsKey('plain') && gitopsConfig.deployments.containsKey('helm')) {
        error 'Please choose between \'deployments.plain\' and \'deployments.helm\'. Setting both properties is not possible!'
        return false
    } else if (!gitopsConfig.deployments.containsKey('plain') && !gitopsConfig.deployments.containsKey('helm')) {
        error 'One of \'deployments.plain\' or \'deployments.helm\' must be set!'
        return false
    }
    
    if (!GitopsTool.get(gitopsConfig.gitopsTool)) {
        error "The specified 'gitopsTool' is invalid. Please choose one of the following: ${GitopsTool.values()}"
    }
    
    if (gitopsConfig.containsKey('folderStructureStrategy') && !FolderStructureStrategy.get(gitopsConfig.folderStructureStrategy)) {
        error "The specified 'folderStructureStrategy' is invalid. Please choose one of the following: ${FolderStructureStrategy.values()}"
    }

    if (gitopsConfig.deployments.containsKey('plain')) {
        deployment = new Plain(this, gitopsConfig)
    } else if (gitopsConfig.deployments.containsKey('helm')) {
        deployment = new Helm(this, gitopsConfig)
    }

    // load the scm-provider that got selected
    switch (gitopsConfig.scm.provider) {
        case 'SCMManager':
            provider = new SCMManager(this)
            provider.setCredentials(gitopsConfig.scm.credentialsId)
            provider.setBaseUrl(gitopsConfig.scm.baseUrl)
            provider.setRepositoryUrl(gitopsConfig.scm.repositoryUrl)
            return true

        default:
            error 'The given scm-provider seems to be invalid. Please choose one of the following: \'SCMManager\'.'
            return false
    }
}

protected initCesBuildLib(cesBuildLibRepo, cesBuildLibVersion, credentialsId) {
    Map retrieverParams = [$class: 'GitSCMSource', remote: cesBuildLibRepo]
    if (credentialsId?.trim()) {
        retrieverParams << [credentialsId: credentialsId]
    }

    return library(identifier: "ces-build-lib@${cesBuildLibVersion}",
        retriever: modernSCM(retrieverParams)
    ).com.cloudogu.ces.cesbuildlib
}

protected void deploy(Map gitopsConfig) {
    def git = cesBuildLib.Git.new(this, gitopsConfig.scm.credentialsId ?: '')
    def gitRepo = prepareGitRepo(git)
    def changesOnGitOpsRepo = ''

    try {
        dir(gitRepo.configRepoTempDir) {

            git url: provider.getRepositoryUrl(), branch: gitopsConfig.mainBranch, changelog: false, poll: false
            git.fetch()

            changesOnGitOpsRepo = aggregateChangesOnGitOpsRepo(syncGitopsRepoPerStage(gitopsConfig, git, gitRepo))
        }
    } finally {
        sh "rm -rf ${gitRepo.configRepoTempDir}"
    }

    if (gitopsConfig.deployments.containsKey('plain')) {
        currentBuild.description = createBuildDescription(changesOnGitOpsRepo, gitopsConfig.deployments.plain.updateImages.imageName as String)
    } else if (gitopsConfig.deployments.containsKey('helm')) {
        currentBuild.description = createBuildDescription(changesOnGitOpsRepo)
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

    gitopsConfig.stages.each { stage, config ->
        //checkout the main_branch before creating a new stage_branch. so it won't be branched off of an already checked out stage_branch
        git.checkoutOrCreate(gitopsConfig.mainBranch)
        if (config.deployDirectly) {
            allRepoChanges += syncGitopsRepo(stage, gitopsConfig.mainBranch, git, gitRepo)
        } else {
            String stageBranch = "${stage}_${gitopsConfig.application}"
            git.checkoutOrCreate(stageBranch)
            String repoChanges = syncGitopsRepo(stage, stageBranch, git, gitRepo)

            if (repoChanges) {
                def title = 'created by service \'' + gitopsConfig.application + '\' for stage \'' + stage + '\''
                //TODO description functionality needs to be implemented
                def description = ''

                provider.createOrUpdatePullRequest(stageBranch, gitopsConfig.mainBranch, title, description)
                allRepoChanges += repoChanges
            }
        }
    }
    return allRepoChanges
}

protected String syncGitopsRepo(String stage, String branch, def git, Map gitRepo) {
    deployment.create(stage)
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

protected String createBuildDescription(String pushedChanges, String imageName) {
    String description = createBuildDescription(pushedChanges)
    description += "\nImage: ${imageName}"
    return description
}

protected String createBuildDescription(String pushedChanges) {
    String description = ''
    description += "GitOps commits: "
    if (pushedChanges) {
        description += pushedChanges
    } else {
        description += 'No changes'
    }
    return description
}

def cesBuildLib
Deployment deployment
SCMProvider provider
