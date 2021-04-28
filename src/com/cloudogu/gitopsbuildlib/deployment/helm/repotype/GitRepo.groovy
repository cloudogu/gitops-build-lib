package com.cloudogu.gitopsbuildlib.deployment.helm.repotype

class GitRepo extends RepoType {

    GitRepo(def script) {
        super(script)
    }

    @Override
    void prepareRepo(Map helmConfig) {

        getHelmChartFromGitRepo(helmConfig)

        def chartPath = ''
        if (helmConfig.containsKey('chartPath')) {
            chartPath = helmConfig.chartPath
        }

        withHelm {
            script.sh "helm dep update chart/${chartPath}"
        }
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
