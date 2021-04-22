package com.cloudogu.gitopsbuildlib.deployment.helm.repotype

class GitRepo extends RepoType {

    GitRepo(def script) {
        super(script)
    }

    @Override
    String mergeValues(Map helmConfig, String[] valuesFiles) {
        String merge = ""

        getHelmChartFromGitRepo(helmConfig)

        def chartPath = ''
        if (helmConfig.containsKey('chartPath')) {
            chartPath = helmConfig.chartPath
        }

        withHelm {
            script.sh "helm dep update chart/${chartPath}"
            String helmScript = "helm values chart/${chartPath} ${valuesFilesWithParameter(valuesFiles)}"
            merge = script.sh returnStdout: true, script: helmScript
        }

        return merge
    }

    private getHelmChartFromGitRepo(Map helmConfig) {
        def git

        script.dir("chart") {

            if (helmConfig.containsKey('credentialsId')) {
                git = script.cesBuildLib.Git.new(script, helmConfig.credentialsId)
            } else {
                git = script.cesBuildLib.Git.new(script)
            }

            git url: helmConfig.repoUrl, branch: 'main', changelog: false, poll: false

            if(helmConfig.containsKey('version') && helmConfig.version) {
                git.fetch()
                git.checkout(helmConfig.version)
            }
        }
    }
}
