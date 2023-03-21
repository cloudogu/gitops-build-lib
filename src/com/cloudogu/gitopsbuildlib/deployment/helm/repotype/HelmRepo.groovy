package com.cloudogu.gitopsbuildlib.deployment.helm.repotype

class HelmRepo extends RepoType {

    HelmRepo(def script) {
        super(script)
    }

    @Override
    String getChartPath(Map gitopsConfig, String helmChartTempDir, String chartRootDir) {
        return "${script.env.WORKSPACE}/${helmChartTempDir}/${chartRootDir}/${gitopsConfig.deployments.helm.chartName}"
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
                addAndPullRepo(gitopsConfig, helmChartTempDir, chartRootDir, credentialArgs)
            }
        } else {
            addAndPullRepo(gitopsConfig, helmChartTempDir, chartRootDir)
        }
    }

    private void addAndPullRepo(Map gitopsConfig, String helmChartTempDir, String chartRootDir, String credentialArgs = "") {
        def helmConfig = gitopsConfig.deployments.helm
        withDockerImage(gitopsConfig.buildImages.helm) {
            script.sh "helm repo add chartRepo ${helmConfig.repoUrl}${credentialArgs}"
            script.sh "helm repo update"
            // helm pull also executes helm dependency so we don't need to do it in this step
            script.sh "helm pull chartRepo/${helmConfig.chartName} --version=${helmConfig.version} --untar --untardir=${script.env.WORKSPACE}/${helmChartTempDir}/${chartRootDir}"
        }
    }
}
