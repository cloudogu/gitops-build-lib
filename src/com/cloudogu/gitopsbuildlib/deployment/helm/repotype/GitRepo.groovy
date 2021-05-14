package com.cloudogu.gitopsbuildlib.deployment.helm.repotype

class GitRepo extends RepoType {

    GitRepo(def script) {
        super(script)
    }

    @Override
    void prepareRepo(Map gitopsConfig, String helmChartTempDir, String chartRootDir) {
        def helmConfig = gitopsConfig.deployments.helm

        getHelmChartFromGitRepo(helmConfig, helmChartTempDir, chartRootDir)

        def chartPath = ''
        if (helmConfig.containsKey('chartPath')) {
            chartPath = helmConfig.chartPath
        }

        withHelm {
            script.sh "helm dep update ${script.env.WORKSPACE}/.helmChartTempDir/${chartRootDir}/${chartPath}"
        }
    }

    private getHelmChartFromGitRepo(Map helmConfig, String helmChartTempDir, String chartRootDir) {
        def git

        script.dir("${script.env.WORKSPACE}/${helmChartTempDir}/${chartRootDir}/") {

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
