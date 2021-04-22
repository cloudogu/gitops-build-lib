package com.cloudogu.gitopsbuildlib.validation

import com.cloudogu.gitopsbuildlib.docker.DockerWrapper

class HelmKubeval extends Validator {

    HelmKubeval(def script) {
        super(script)
    }

    @Override
    void validate(String targetDirectory, Map config, Map deployments) {
        if (deployments.containsKey('helm')) {
            if (deployments.helm.repoType == 'GIT') {
//                script.dir("${targetDirectory}/chart") {
//                    def git = (deployments.helm.containsKey('credentialsId'))
//                        ? script.cesBuildLib.Git.new(script, deployments.helm.credentialsId)
//                        : script.cesBuildLib.Git.new(script)
//                    git url: deployments.helm.repoUrl, branch: 'main', changelog: false, poll: false
//
//                    if(deployments.helm.containsKey('version') && deployments.helm.version) {
//                        git.checkout(deployments.helm.version)
//                    }
//                }

                def chartPath = ''
                if (deployments.helm.containsKey('chartPath')) {
                    chartPath = deployments.helm.chartPath
                }

                withDockerImage(config.image) {
                    script.sh "helm kubeval chart/${chartPath} -v ${config.k8sSchemaVersion}"
                }

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
