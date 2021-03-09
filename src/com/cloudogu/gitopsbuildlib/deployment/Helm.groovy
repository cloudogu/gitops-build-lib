package com.cloudogu.gitopsbuildlib.deployment

import com.cloudogu.gitopsbuildlib.deployment.repotype.GitRepo
import com.cloudogu.gitopsbuildlib.deployment.repotype.HelmRepo
import com.cloudogu.gitopsbuildlib.deployment.repotype.RepoType

class Helm extends Deployment {

    protected RepoType helm

    Helm(def script, def gitopsConfig) {
        super(script, gitopsConfig)
        if(gitopsConfig.deployments.helm.repoType == 'GIT') {
            helm = new GitRepo(script, gitopsConfig)
        } else if (gitopsConfig.deployments.helm.repoType == 'HELM') {
            helm = new HelmRepo(script, gitopsConfig)
        }
    }

    @Override
    def createPreValidation(String stage) {
        helm.createRelease(stage)
    }

    @Override
    def createPostValidation(String stage) {
    }
}
