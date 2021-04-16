package com.cloudogu.gitopsbuildlib.deployment.helm.helmrelease

class ArgoCDGitRepoRelease extends HelmRelease{

    protected static String getHelmImage() { 'ghcr.io/cloudogu/helm:3.5.4-1' }

    ArgoCDGitRepoRelease(def script) {
        super(script)
    }

    @Override
    String create(Map helmConfig, String application, String namespace, String valuesFile) {
        def credentialsId = helmConfig.credentialsId
        def myGit = script.cesBuildLib.Git.new(this, credentialsId)

        script.dir("${script.env.WORKSPACE}/helmChart") {
            script.git credentialsId: credentialsId, url: helmConfig.helmChartRepository, branch: 'main', changelog: false, poll: false
            if(helmConfig.containsKey('version') && helmConfig.version) {
                myGit.fetch()
                myGit.checkout(helmConfig.version)
            }
        }

        String helmRelease = ""

        withHelm {
            script.dir("${script.env.WORKSPACE}/helmChart") {
                script.sh "helm dep update ."
                String templateScript = "helm template ${application} . -f ${valuesFile}"
                helmRelease = script.sh returnStdout: true, script: templateScript
            }
        }

        return helmRelease
    }

    void withHelm(Closure body) {
        script.cesBuildLib.Docker.new(script).image(helmImage).inside(
            "${script.pwd().equals(script.env.WORKSPACE) ? '' : "-v ${script.env.WORKSPACE}:${script.env.WORKSPACE}"}"
        ) {
            body()
        }
    }
}
