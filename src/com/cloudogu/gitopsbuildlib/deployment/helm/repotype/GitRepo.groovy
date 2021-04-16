package com.cloudogu.gitopsbuildlib.deployment.helm.repotype

class GitRepo extends RepoType {

    GitRepo(def script) {
        super(script)
    }

    @Override
    String mergeValues(Map helmConfig, String[] files) {
        String merge = ""
        String _files = ""
        files.each {
            _files += "-f $it "
        }

        def myGit = script.cesBuildLib.Git.new(this, helmConfig.credentialsId)

        script.dir("${script.env.WORKSPACE}/chart") {
            if (helmConfig.containsKey('credentialsId')) {
                script.git credentialsId: helmConfig.credentialsId, url: helmConfig.repoUrl, branch: 'main', changelog: false, poll: false
                myGit.fetch()
                myGit.checkout(helmConfig.version)
            } else {
                script.git url: helmConfig.repoUrl, branch: 'main', changelog: false, poll: false
                myGit.fetch()
                myGit.checkout(helmConfig.version)
            }
        }

        def chartPath = ''
        if (helmConfig.containsKey('chartPath')) {
            chartPath = helmConfig.chartPath
        }

        withHelm {
            String helmScript = "helm values ${script.env.WORKSPACE}/chart/${chartPath} ${_files}"
            merge = script.sh returnStdout: true, script: helmScript
        }

        script.sh "rm -rf ${script.env.WORKSPACE}/chart || true"

        return merge
    }
}
