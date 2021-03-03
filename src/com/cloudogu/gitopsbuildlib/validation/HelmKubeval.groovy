package com.cloudogu.gitopsbuildlib.validation

class HelmKubeval extends Validator {

    HelmKubeval(def script) {
        super(script)
    }

    @Override
    void validate(String targetDirectory, Map config, Map deployments) {
        if (deployments.containsKey('helm')) {

            if(deployments.helm.repoType == 'HELM') {
                cloneGitHelmRepo(deployments.helm.repoUrl, deployments.helm.version, targetDirectory)
            }

            withDockerImage(config.image) {
                script.sh "helm kubeval ${targetDirectory}/chart -v ${config.k8sSchemaVersion} --strict"
            }

            script.sh "rm -rf ${targetDirectory}/chart"
        }
    }

    private void cloneGitHelmRepo(String repoUrl, String version, String targetDirectory) {
        script.sh "git clone ${repoUrl} ${targetDirectory}/chart || true"
        script.sh "cd ${targetDirectory}/chart"
        script.sh "git checkout ${version}"
    }
}
