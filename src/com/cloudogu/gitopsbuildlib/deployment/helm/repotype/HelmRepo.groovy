package com.cloudogu.gitopsbuildlib.deployment.helm.repotype

class HelmRepo extends RepoType{

    HelmRepo(def script) {
        super(script)
    }

    @Override
    String mergeValues(Map helmConfig, String[] valuesFiles) {
        String merge = ""

        withHelm {
            script.sh "helm repo add chartRepo ${helmConfig.repoUrl}"
            script.sh "helm repo update"
            script.sh "helm pull chartRepo/${helmConfig.chartName} --version=${helmConfig.version} --untar --untardir=${script.env.WORKSPACE}/chart"
            String helmScript = "helm values ${script.env.WORKSPACE}/chart/${helmConfig.chartName} ${valuesFilesWithParameter(valuesFiles)}"
            merge = script.sh returnStdout: true, script: helmScript
        }

        script.sh "rm -rf ${script.env.WORKSPACE}/chart || true"

        return merge
    }
}
