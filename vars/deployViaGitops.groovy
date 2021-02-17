#!groovy

import com.cloudogu.gitopsbuildlib.*

String getConfigDir() { '.config' }


void call(Map gitopsConfig) {
  cesBuildLib = initCesBuildLib(gitopsConfig.cesBuildLibRepo, gitopsConfig.cesBuildLibVersion)
  deploy(gitopsConfig)
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

  currentBuild.description = createBuildDescription(changesOnGitOpsRepo, gitopsConfig.imageName)
}

protected Map prepareGitRepo(def git) {
  // Query and store info about application repo before cloning into gitops repo
  def applicationRepo = GitRepo.create(git)

  // Display that Jenkins made the GitOps commits, not the application repo author
  git.committerName = 'Jenkins'
  git.committerEmail = 'jenkins@cloudogu.com'

  def configRepoTempDir = '.configRepoTempDir'

  return [
          applicationRepo: applicationRepo,
          configRepoTempDir: configRepoTempDir
  ]
}

protected HashSet<String> syncGitopsRepoPerStage(Map gitopsConfig, def git, Map gitRepo) {

  HashSet<String> allRepoChanges = new HashSet<String>()

  gitopsConfig.stages.each{ stage, config ->
    //checkout the main_branch before creating a new stage_branch. so it won't be branched off of an already checked out stage_branch
    git.checkoutOrCreate(gitopsConfig.mainBranch)
    if(config.deployDirectly) {
      allRepoChanges += syncGitopsRepo(stage, gitopsConfig.mainBranch, git, gitRepo, gitopsConfig)
    } else {
      String stageBranch = "${stage}_${gitopsConfig.application}"
      git.checkoutOrCreate(stageBranch)
      String repoChanges = syncGitopsRepo(stage, stageBranch, git, gitRepo, gitopsConfig)

      if(repoChanges) {
        createPullRequest(gitopsConfig, stage, stageBranch)
        allRepoChanges += repoChanges
      }
    }
  }
  return allRepoChanges
}

protected String syncGitopsRepo(String stage, String branch, def git, Map gitRepo, Map gitopsConfig) {

  createApplicationFolders(stage, gitopsConfig)

  // TODO user decides if validation is necessary
  def validate = new ValidateResources("${stage}/${gitopsConfig.application}/", "${configDir}/config.yamllint.yaml", cesBuildLib)
  validate.start()

  gitopsConfig.updateImages.each {
    updateImageVersion("${stage}/${gitopsConfig.application}/${it['deploymentFilename']}", it['containerName'], it['imageName'])
  }

  return commitAndPushToStage(stage, branch, git, gitRepo)
}

private void createApplicationFolders(String stage, Map gitopsConfig) {
  sh "mkdir -p ${stage}/${gitopsConfig.application}/"
  sh "mkdir -p ${configDir}/"
  // copy extra resources like sealed secrets
  echo "Copying k8s payload from application repo to gitOps Repo: 'k8s/${stage}/*' to '${stage}/${gitopsConfig.application}'"
  sh "cp ${env.WORKSPACE}/k8s/${stage}/* ${stage}/${gitopsConfig.application}/ || true"
  sh "cp ${env.WORKSPACE}/*.yamllint.yaml ${configDir}/ || true"
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
  String issueIds =  (applicationRepo.commitMessage =~ /#\d*/).collect { "${it} " }.join('')

  String[] urlSplit = applicationRepo.repositoryUrl.split('/')
  def repoNamespace = urlSplit[-2]
  def repoName = urlSplit[-1]
  String message = "${issueIds}${repoNamespace}/${repoName}@${applicationRepo.commitHash}"

  return message
}

private void createPullRequest(Map gitopsConfig, String stage, String sourceBranch) {

  withCredentials([usernamePassword(credentialsId: gitopsConfig.scmmCredentialsId, passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USER')]) {

    String script =
            'curl -s -o /dev/null -w "%{http_code}" ' +
                    "-u ${GIT_USER}:${GIT_PASSWORD} " +
                    '-H "Content-Type: application/vnd.scmm-pullRequest+json;v=2" ' +
                    '--data \'{"title": "created by service ' + gitopsConfig.application + ' for stage ' + stage + '", "source": "' + sourceBranch + '", "target": "' + gitopsConfig.mainBranch + '"}\' ' +
                    gitopsConfig.scmmPullRequestUrl

    // For debugging the quotation of the shell script, just do: echo script
    String http_code = sh returnStdout: true, script: script

    // At this point we could write a mail to the last committer that his commit triggered a new or updated GitOps PR

    echo "http_code: ${http_code}"
    // PR exists if we get 409
    if (http_code != "201" && http_code != "409") {
      unstable 'Could not create pull request'
    }
  }
}

private void updateImageVersion(String deploymentFilePath, String containerName, String newImageTag) {
  def data = readYaml file: deploymentFilePath
  def containers = data.spec.template.spec.containers
  def updateContainer = containers.find {it.name == containerName}
  updateContainer.image = newImageTag
  writeYaml file: deploymentFilePath, data: data, overwrite: true
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
