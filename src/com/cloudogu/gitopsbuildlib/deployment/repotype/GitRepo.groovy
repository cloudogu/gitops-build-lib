package com.cloudogu.gitopsbuildlib.deployment.repotype

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

        script.dir("${script.env.WORKSPACE}/chart") {
            if (helmConfig.containsKey('credentialsId')) {
                script.git credentialsId: helmConfig.credentialsId, url: helmConfig.repoUrl, branch: 'main', changelog: false, poll: false
            } else {
                script.git url: helmConfig.repoUrl, branch: 'main', changelog: false, poll: false
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

    @Override
    String createHelmRelease(Map helmConfig, String application, String namespace, String valuesFile) {
        def values = fileToInlineYaml(valuesFile)
        return """apiVersion: helm.fluxcd.io/v1
kind: HelmRelease
metadata:
  name: ${application}
  namespace: ${namespace}
  annotations:
    fluxcd.io/automated: "false"
spec:
  releaseName: ${application}
  chart:
    git: ${helmConfig.repoUrl}
    ref: ${helmConfig.version}
    path: .
  values:
${values}
"""
    }
}
