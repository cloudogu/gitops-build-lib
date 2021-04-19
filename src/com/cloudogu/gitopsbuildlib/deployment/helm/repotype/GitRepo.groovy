package com.cloudogu.gitopsbuildlib.deployment.helm.repotype

class GitRepo extends RepoType {

    GitRepo(def script) {
        super(script)
    }

    @Override
    String mergeValues(Map helmConfig, String[] valuesFiles) {
        String merge = ""

        prepareGitRepo(helmConfig)

        def chartPath = ''
        if (helmConfig.containsKey('chartPath')) {
            chartPath = helmConfig.chartPath
        }

        withHelm {
            String helmScript = "helm values ${script.env.WORKSPACE}/chart/${chartPath} ${valuesFilesWithParameter(valuesFiles)}"
            merge = script.sh returnStdout: true, script: helmScript
        }

        return merge
    }

    private prepareGitRepo(Map helmConfig) {
        def myGit

        script.dir("${script.env.WORKSPACE}/chart") {

            if (helmConfig.containsKey('credentialsId')) {
                script.git credentialsId: helmConfig.credentialsId, url: helmConfig.repoUrl, branch: 'main', changelog: false, poll: false
                myGit = script.cesBuildLib.Git.new(script, helmConfig.credentialsId)
            } else {
                script.git url: helmConfig.repoUrl, branch: 'main', changelog: false, poll: false
                myGit = script.cesBuildLib.Git.new(script)
            }
            if(helmConfig.containsKey('version') && helmConfig.version) {
                myGit.fetch()
                myGit.checkout(helmConfig.version)
            }
        }
    }
}
