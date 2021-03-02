package com.cloudogu.gitopsbuildlib.validation

class HelmKubeval extends Validator {

    HelmKubeval(def script) {
        super(script)
    }

    @Override
    void validate(String targetDirectory, Map config, Map deployments) {
        if (deployments.containsKey('helm')) {

            //TODO branch / tag / commit clone
            script.sh "git clone ${deployments.helm.repoUrl} ${targetDirectory}/chart || true"

            withDockerImage(config.image) {
                script.sh "helm kubeval ${targetDirectory}/chart -v ${config.k8sSchemaVersion} --strict"
            }

            script.sh "rm -rf ${targetDirectory}/chart"
        }
    }
}
