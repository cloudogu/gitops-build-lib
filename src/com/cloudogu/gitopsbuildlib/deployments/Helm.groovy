package com.cloudogu.gitopsbuildlib.deployments

class Helm extends Deployment {


    protected RepoType repo

    Helm(def script, def gitopsConfig) {
        super(script, gitopsConfig)
        if(gitopsConfig.deployments.helm.repoType == 'GIT') {
            repo = new GitRepo()
        } else if (gitopsConfig.deployments.helm.repoType == 'HELM') {
            repo = new HelmRepo()
        }
    }

    @Override
    process(String stage) {
        repo.generateFoldersAndFiles(stage, gitopsConfig)
        validate(stage)
    }
}
