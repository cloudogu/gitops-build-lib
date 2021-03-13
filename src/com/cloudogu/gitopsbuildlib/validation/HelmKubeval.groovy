package com.cloudogu.gitopsbuildlib.validation

class HelmKubeval extends Validator {

    HelmKubeval(def script) {
        super(script)
    }

    @Override
    void validate(String targetDirectory, Map config, Map deployments) {
        if (deployments.containsKey('helm')) {
            if (deployments.helm.repoType == 'GIT') {
                processGitRepo(targetDirectory, config, deployments)
            } else if (deployments.helm.repoType == 'HELM') {
                processHelmRepo(config, deployments)
            }
        }
    }

    private void processGitRepo(String targetDirectory, Map config, Map deployments) {
        cloneGitHelmRepo(deployments.helm, targetDirectory)

        def chartPath = ''
        if(deployments.helm.containsKey('chartPath')) {
            chartPath = deployments.helm.chartPath
        }

        withDockerImage(config.image) {
            script.sh "helm kubeval ${targetDirectory}/chart/${chartPath} -v ${config.k8sSchemaVersion}"
        }
        script.sh "rm -rf ${targetDirectory}/chart"
    }

    private void processHelmRepo(Map config, Map deployments) {
        withDockerImage(config.image) {
            script.sh "helm repo add chartRepo ${deployments.helm.repoUrl}"
            script.sh "helm repo update"
            script.sh "helm kubeval chartRepo/${deployments.helm.chartName} --version=${deployments.helm.version} -v ${config.k8sSchemaVersion}"
        }
    }

    private void cloneGitHelmRepo(Map helmConfig, String targetDirectory) {
        script.sh "git clone ${helmConfig.repoUrl} ${targetDirectory}/chart || true"
        script.sh "git --git-dir=${targetDirectory}/chart/.git --work-tree=${targetDirectory}/chart checkout ${helmConfig.version}"
    }
}
