package com.cloudogu.gitopsbuildlib.validation

class HelmKubeval extends Validator {

    HelmKubeval(def script) {
        super(script)
    }

    @Override
    void validate(String targetDirectory, Map config, Map deployments) {
        if (deployments.containsKey('helm')) {

            def chartDir = ''
            if (deployments.helm.containsKey('chartPath')) {
                chartDir = deployments.helm.chartPath
            } else if ( deployments.helm.containsKey('chartName')) {
                chartDir = deployments.helm.chartName
            }

            withDockerImage(config.image) {
                script.sh "helm kubeval chart/${chartDir} -v ${config.k8sSchemaVersion}"
            }
        }
    }
}
