package com.cloudogu.gitopsbuildlib.validation

class HelmKubeval extends Validator {

    HelmKubeval(def script) {
        super(script)
    }

    @Override
    void validate(String targetDirectory, Map config, Map deployments) {
        if (deployments.containsKey('helm')) {

            if (deployments.helm.repoType == 'GIT') {
                cloneGitHelmRepo(deployments.helm, targetDirectory)
                withDockerImage(config.image) {
                    script.sh "helm kubeval ${targetDirectory}/chart -v ${config.k8sSchemaVersion}"
                }
                script.sh "rm -rf ${targetDirectory}/chart"
            }
            if (deployments.helm.repoType == 'HELM')
                withDockerImage(config.image) {
                    script.sh "helm add repo chartRepo ${deployments.helm.repoUrl}"
                    script.sh "helm repo update"
                    script.sh "helm kubeval chartRepo/${deployments.helm.chartName} --version=${deployments.helm.version} -v ${config.k8sSchemaVersion}"
                }
        }
    }

    private void cloneGitHelmRepo(Map helmConfig, String targetDirectory) {
        script.sh "git clone ${helmConfig.repoUrl} ${targetDirectory}/${helmConfig.chartPath} || true"
        def repoPath = helmConfig.repoUrl
        repoPath = repoPath.substring(repoPath.lastIndexOf('/') + 1)
        script.sh "cd ${targetDirectory}/${helmConfig.chartPath}/${repoPath}"
        script.sh "git checkout ${helmConfig.version}"
    }
}
