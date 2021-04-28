package com.cloudogu.gitopsbuildlib.deployment.helm.repotype

class HelmRepo extends RepoType{

    HelmRepo(def script) {
        super(script)
    }

    @Override
    void prepareRepo(Map helmConfig) {

        if (helmConfig.containsKey('credentialsId') && helmConfig.credentialsId) {
            script.withCredentials([
                script.usernamePassword(
                    credentialsId: helmConfig.credentialsId,
                    usernameVariable: 'USERNAME',
                    passwordVariable: 'PASSWORD')
            ]) {
                String credentialArgs = " --username ${script.USERNAME} --password ${script.PASSWORD}"
                addAndPullRepo(helmConfig, credentialArgs)
            }
        } else {
            addAndPullRepo(helmConfig)
        }
    }

    private void addAndPullRepo(Map helmConfig, String credentialArgs = "") {
        withHelm {
            script.sh "helm repo add chartRepo ${helmConfig.repoUrl}${credentialArgs}"
            script.sh "helm repo update"
            // helm pull also executes helm dependency so we don't need to do it in this step
            script.sh "helm pull chartRepo/${helmConfig.chartName} --version=${helmConfig.version} --untar --untardir=chart"
        }
    }
}
