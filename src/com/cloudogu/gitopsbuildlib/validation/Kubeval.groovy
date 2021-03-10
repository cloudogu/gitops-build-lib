package com.cloudogu.gitopsbuildlib.validation

/**
 * Validates all yaml-resources within the target-directory against the specs of the given k8s version
 */
class Kubeval extends Validator {

    Kubeval(def script) {
        super(script)
    }

    @Override
    void validate(String targetDirectory, Map config, Map deployments) {
        if (deployments.containsKey('plain')) {
            withDockerImage(config.image) {
                script.sh "kubeval -d ${targetDirectory} -v ${config.k8sSchemaVersion} --strict"
            }
        }
    }
}
