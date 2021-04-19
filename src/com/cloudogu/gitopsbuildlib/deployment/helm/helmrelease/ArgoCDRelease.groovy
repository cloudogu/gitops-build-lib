package com.cloudogu.gitopsbuildlib.deployment.helm.helmrelease

class ArgoCDRelease extends HelmRelease{

    protected static String getHelmImage() { 'ghcr.io/cloudogu/helm:3.5.4-1' }

    ArgoCDRelease(def script) {
        super(script)
    }

    @Override
    String create(Map helmConfig, String application, String namespace, String valuesFile) {

        String valuesFileLocation = "${script.env.WORKSPACE}/.configRepoTempDir/${valuesFile}"
        String helmRelease = ""
        if (helmConfig.repoType == 'GIT') {
            helmRelease = gitRepoRelease(helmConfig, application, valuesFileLocation)
        } else if (helmConfig.repoType == 'HELM') {
            // TODO not yet implemented
        }
        return helmRelease
    }

    private String gitRepoRelease(Map helmConfig, String application, String valuesFileLocation) {
        String helmRelease = ""

        def chartPath = ''
        if (helmConfig.containsKey('chartPath')) {
            chartPath = helmConfig.chartPath
        }

        withHelm {
            script.dir("${script.env.WORKSPACE}/chart/${chartPath}") {
                script.sh "helm dep update ."
                String templateScript = "helm template ${application} . -f ${valuesFileLocation}"
                helmRelease = script.sh returnStdout: true, script: templateScript
            }
        }
        // this line removes all empty lines since helm template creates some and the helm validator will throw an error if there are emtpy lines present
        helmRelease.replaceAll("(?m)^[ \t]*\r?\n", "")
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
