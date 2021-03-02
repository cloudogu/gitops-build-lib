package com.cloudogu.gitopsbuildlib.validation

class HelmKubeval extends Validator {

    HelmKubeval(def script) {
        super(script)
    }

    @Override
    void validate(String targetDirectory, Map config, Map deployments) {
        if (deployments.containsKey('helm')) {
            withDockerImage(config.image) {
                script.sh "helm kubeval -d ${targetDirectory} -v ${config.k8sSchemaVersion} --strict"
            }
        }
    }
}
