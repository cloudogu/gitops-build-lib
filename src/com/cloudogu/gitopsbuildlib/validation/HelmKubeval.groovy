package com.cloudogu.gitopsbuildlib.validation

class HelmKubeval extends Validator {

    HelmKubeval(def script) {
        super(script)
    }

    @Override
    void validate(String targetDirectory, Map config, Map deployments) {
        if (deployments.containsKey('helm')) {
            if (deployments.helm.repoType == 'GIT') {
                script.dir("${targetDirectory}/chart") {
                    def git = (deployments.helm.containsKey('credentialsId'))
                        ? script.cesBuildLib.Git.new(script, deployments.helm.credentialsId)
                        : script.cesBuildLib.Git.new(script)
                    git url: deployments.helm.repoUrl, branch: 'main', changelog: false, poll: false
                    git.checkout(deployments.helm.version)
                }

                def chartPath = ''
                if (deployments.helm.containsKey('chartPath')) {
                    chartPath = deployments.helm.chartPath
                }

                withDockerImage(config.image) {
                    script.sh "helm kubeval ${targetDirectory}/chart/${chartPath} -v ${config.k8sSchemaVersion}"
                }

                script.sh "rm -rf ${targetDirectory}/chart"
            } else if (deployments.helm.repoType == 'HELM') {
                withDockerImage(config.image) {
                    script.sh "helm repo add chartRepo ${deployments.helm.repoUrl}"
                    script.sh "helm repo update"
                    script.sh "helm kubeval chartRepo/${deployments.helm.chartName} --version=${deployments.helm.version} -v ${config.k8sSchemaVersion}"
                }
            }
        }
    }
}
