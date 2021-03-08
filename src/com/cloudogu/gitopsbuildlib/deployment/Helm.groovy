package com.cloudogu.gitopsbuildlib.deployment

import com.cloudogu.gitopsbuildlib.deployment.repotype.GitRepo
import com.cloudogu.gitopsbuildlib.deployment.repotype.HelmRepo
import com.cloudogu.gitopsbuildlib.deployment.repotype.RepoType

class Helm extends Deployment {

    protected RepoType helmRepo

    Helm(def script, def gitopsConfig) {
        super(script, gitopsConfig)
        if(gitopsConfig.deployments.helm.repoType == 'GIT') {
            helmRepo = new GitRepo(script, gitopsConfig)
        } else if (gitopsConfig.deployments.helm.repoType == 'HELM') {
            helmRepo = new HelmRepo(script, gitopsConfig)
        }
    }

    @Override
    preparePreValidation(String stage) {
        helmRepo.createHelmDeployment(stage)
    }

    @Override
    def preparePostValidation(String stage) {
    }
}
