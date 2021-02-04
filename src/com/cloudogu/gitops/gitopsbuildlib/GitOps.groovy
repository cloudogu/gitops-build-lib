package com.cloudogu.gitops.gitopsbuildlib

class GitOps implements Serializable{
  // "Constants
  String getK8sVersion() { '1.18.1 '}
  String getConfigDir() { '.config'}
  String getHelmImage() { 'ghcr.io/cloudogu/helm:3.4.1-1'}
  String getYamlLintImage() { 'cytopia/yamllint:1.25' }

  private script
  private cesBuildLib
  private scmManagerCredentials
  private application

  GitOps(script, application, cesBuildLibRepo, cesBuildLibVersion, scmManagerCredentials) {
    this.script = script
    this.application = application
    initCesBuildLib(cesBuildLibRepo, cesBuildLibVersion)
    this.scmManagerCredentials = scmManagerCredentials
  }

  private initCesBuildLib(cesBuildLibRepo, cesBuildLibVersion) {
    this.cesBuildLib = script.library(identifier: "ces-build-lib@${cesBuildLibVersion}",
        retriever: script.modernSCM([$class: 'GitSCMSource', remote: cesBuildLibRepo])
    ).com.cloudogu.ces.cesbuildlib
  }

  String createBuildDescription(String pushedChanges, String imageName) {
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

  String createImageTag() {
    def git = cesBuildLib.Git.new(this)
    String branch = git.simpleBranchName
    String branchSuffix = ""

    if (!"develop".equals(branch)) {
      branchSuffix = "-${branch}"
    }

    return "${new Date().format('yyyyMMddHHmm')}-${git.commitHashShort}${branchSuffix}"
  }

  String pushToConfigRepo(Map gitopsConfig) {

    def git = cesBuildLib.Git.new(this, scmManagerCredentials)
    def changesOnGitOpsRepo = ''

    // Query and store info about application repo before cloning into gitops repo
    def applicationRepo = GitRepo.create(git)

    // Display that Jenkins made the GitOps commits not the application repo author
    git.committerName = 'Jenkins'
    git.committerEmail = 'jenkins@cloudogu.com'

    def configRepoTempDir = '.configRepoTempDir'

    try {

      dir(configRepoTempDir) {

        git url: gitopsConfig.scmmConfigRepoUrl, branch: gitopsConfig.mainBranch, changelog: false, poll: false
        git.fetch()

        def allRepoChanges = new HashSet<String>()

        gitopsConfig.stages.each{ stage, config ->
          //checkout the main_branch before creating a new stage_branch. so it won't be branched off of an already checked out stage_branch
          git.checkoutOrCreate(mainBranch)

          if(config.deployDirectly) {
            allRepoChanges += createApplicationForStageAndPushToBranch stage as String, mainBranch, applicationRepo, git, gitopsConfig
          } else {
            String stageBranch = "${stage}_${application}"
            git.checkoutOrCreate(stageBranch)
            String repoChanges = createApplicationForStageAndPushToBranch stage as String, stageBranch, applicationRepo, git, gitopsConfig
            if(repoChanges) {
              createPullRequest(gitopsConfig, stage as String, stageBranch)
              allRepoChanges += repoChanges
            }
          }
        }
        changesOnGitOpsRepo = aggregateChangesOnGitOpsRepo(allRepoChanges)
      }
    } finally {
      sh "rm -rf ${configRepoTempDir}"
    }

    return changesOnGitOpsRepo
  }

  private String createApplicationForStageAndPushToBranch(String stage, String branch, GitRepo applicationRepo, def git, Map gitopsConfig) {

    String commitPrefix = "[${stage}] "

    sh "mkdir -p ${stage}/${application}/"
    sh "mkdir -p ${configDir}/"
    // copy extra resources like sealed secrets
    echo "Copying k8s payload from application repo to gitOps Repo: 'k8s/${stage}/*' to '${stage}/${application}'"
    sh "cp ${env.WORKSPACE}/k8s/${stage}/* ${stage}/${application}/ || true"
    sh "cp ${env.WORKSPACE}/*.yamllint.yaml ${configDir}/ || true"

    validateK8sRessources("${stage}/${application}/", k8sVersion)
    validateYamlResources("${configDir}/config.yamllint.yaml", "${stage}/${application}/")

    gitopsConfig.updateImages.each {
      updateImageVersion("${stage}/${application}/${it['deploymentFilename']}", it['containerName'], it['imageName'])
    }

    git.add('.')
    if (git.areChangesStagedForCommit()) {
      git.commit(commitPrefix + createApplicationCommitMessage(git, applicationRepo), applicationRepo.authorName, applicationRepo.authorEmail)

      // If some else pushes between the pull above and this push, the build will fail.
      // So we pull if push fails and try again
      git.pushAndPullOnFailure("origin ${branch}")
      return "${stage} (${git.commitHashShort})"
    } else {
      echo "No changes on gitOps repo for ${stage} (branch: ${branch}). Not committing or pushing."
      return ''
    }
  }

  private String aggregateChangesOnGitOpsRepo(changes) {
    // Remove empty
    (changes - '')
    // and concat into string
            .join('; ')
  }

  private String createApplicationCommitMessage(def git, def applicationRepo) {
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
                      '--data \'{"title": "created by service ' + application + ' for stage ' + stage + '", "source": "' + sourceBranch + '", "target": "' + mainBranch + '"}\' ' +
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

  // Validates all yaml-resources within the target-directory against the specs of the given k8s version
  private void validateK8sRessources(String targetDirectory, String k8sVersion) {
    withDockerImage(helmImage) {
      result = sh returnStdout: true, script: "kubeval -d ${targetDirectory} -v ${k8sVersion} --strict"
    }
  }

  private void validateYamlResources(String configFile, String targetDirectory) {
    withDockerImage(yamlLintImage) {
      sh returnStdout: true, script: "yamllint -c ${configFile} ${targetDirectory}"
    }
  }

  private void withDockerImage(String image, Closure body) {
    cesBuildLib.Docker.new(this).image(image)
    // Allow accessing WORKSPACE even when we are in a child dir (using "dir() {}")
            .inside("${pwd().equals(env.WORKSPACE) ? '' : "-v ${env.WORKSPACE}:${env.WORKSPACE}"}") {
              body()
            }
  }

/** Queries and stores info about current repo and HEAD commit */
  class GitRepo {

    static GitRepo create(git) {
      // Constructors can't be used in Jenkins pipelines due to CPS
      // https://www.jenkins.io/doc/book/pipeline/cps-method-mismatches/#constructors
      return new GitRepo(git.commitAuthorName, git.commitAuthorEmail ,git.commitHashShort, git.commitMessage, git.repositoryUrl)
    }

    GitRepo(String authorName, String authorEmail, String commitHash, String commitMessage, String repositoryUrl) {
      this.authorName = authorName
      this.authorEmail = authorEmail
      this.commitHash = commitHash
      this.commitMessage = commitMessage
      this.repositoryUrl = repositoryUrl
    }

    final String authorName
    final String authorEmail
    final String commitHash
    final String commitMessage
    final String repositoryUrl
  }
}
