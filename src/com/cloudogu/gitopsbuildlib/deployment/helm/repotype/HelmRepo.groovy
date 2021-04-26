package com.cloudogu.gitopsbuildlib.deployment.helm.repotype

class HelmRepo extends RepoType{

    HelmRepo(def script) {
        super(script)
    }

    @Override
    String mergeValues(Map helmConfig, String[] valuesFiles) {

        if (helmConfig.containsKey('credentialsId') && helmConfig.credentialsId) {
            script.withCredentials([
                script.usernamePassword(
                    credentialsId: helmConfig.credentialsId,
                    usernameVariable: 'USERNAME',
                    passwordVariable: 'PASSWORD')
            ]) {
                String credentialArgs = " --username ${script.USERNAME} --password ${script.PASSWORD}"
                return mergeValuesFiles(helmConfig, valuesFiles, credentialArgs)
            }
        } else {
            return mergeValuesFiles(helmConfig, valuesFiles)
        }
    }

    private String mergeValuesFiles(Map helmConfig, String[] valuesFiles, String credentialArgs = "") {
        String merge = ""

        withHelm {
            script.sh "helm repo add chartRepo ${helmConfig.repoUrl}${credentialArgs}"
            script.sh "helm repo update"
            // helm pull also executes helm dependency so we don't need to do it in this step
            script.sh "helm pull chartRepo/${helmConfig.chartName} --version=${helmConfig.version} --untar --untardir=chart"
            String helmScript = "helm values chart/${helmConfig.chartName} ${valuesFilesWithParameter(valuesFiles)}"
            merge = script.sh returnStdout: true, script: helmScript
        }

        return merge
    }
}
