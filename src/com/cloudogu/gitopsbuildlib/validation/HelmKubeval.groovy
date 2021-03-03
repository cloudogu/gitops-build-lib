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
                    script.sh "helm kubeval ${targetDirectory}/${deployments.helm.chartPath} -v ${config.k8sSchemaVersion}"
                }
                script.sh "rm -rf ${targetDirectory}/${deployments.helm.chartPath}"
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
        script.sh "git --git-dir=${targetDirectory}/${helmConfig.chartPath}/.git --work-tree=${targetDirectory}/${helmConfig.chartPath} checkout ${helmConfig.version}"
    }
}
