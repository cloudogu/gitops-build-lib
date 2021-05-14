package com.cloudogu.gitopsbuildlib.deployment.helm.repotype

class HelmRepo extends RepoType {

    HelmRepo(def script) {
        super(script)
    }

    @Override
    void prepareRepo(Map gitopsConfig, String helmChartTempDir, String chartRootDir) {
        def helmConfig = gitopsConfig.deployments.helm

        if (helmConfig.containsKey('credentialsId') && helmConfig.credentialsId) {
            script.withCredentials([
                script.usernamePassword(
                    credentialsId: helmConfig.credentialsId,
                    usernameVariable: 'USERNAME',
                    passwordVariable: 'PASSWORD')
            ]) {
                String credentialArgs = " --username ${script.USERNAME} --password ${script.PASSWORD}"
                addAndPullRepo(helmConfig, helmChartTempDir, chartRootDir, credentialArgs)
            }
        } else {
            addAndPullRepo(helmConfig, helmChartTempDir, chartRootDir)
        }
    }

    private void addAndPullRepo(Map helmConfig, String helmChartTempDir, String chartRootDir, String credentialArgs = "") {
        withHelm {
            script.sh "helm repo add chartRepo ${helmConfig.repoUrl}${credentialArgs}"
            script.sh "helm repo update"
            // helm pull also executes helm dependency so we don't need to do it in this step
            script.sh "helm pull chartRepo/${helmConfig.chartName} --version=${helmConfig.version} --untar --untardir=${script.env.WORKSPACE}/${helmChartTempDir}/${chartRootDir}"
        }
    }
}
