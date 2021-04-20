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
            helmRelease = helmRepoRelease(helmConfig, application, valuesFileLocation)
        }
        return helmRelease
    }

    private String gitRepoRelease(Map helmConfig, String application, String valuesFileLocation) {

        def chartPath = ''
        if (helmConfig.containsKey('chartPath')) {
            chartPath = helmConfig.chartPath
        }

        return createHelmRelease(chartPath as String, application, valuesFileLocation)
    }

    private String helmRepoRelease(Map helmConfig, String application, String valuesFileLocation) {
        return createHelmRelease(helmConfig.chartName as String, application, valuesFileLocation)
    }

    private String createHelmRelease(String chartPath, String application, String valuesFileLocation) {
        String helmRelease = ""
        withHelm {
            script.dir("${script.env.WORKSPACE}/chart/${chartPath}") {
                script.sh "helm dep update ."
                String templateScript = "helm template ${application} . -f ${valuesFileLocation}"
                helmRelease = script.sh returnStdout: true, script: templateScript
            }
        }
        // this line removes all empty lines since helm template creates some and the helm validator will throw an error if there are emtpy lines present
        helmRelease = helmRelease.replaceAll("(?m)^[ \t]*\r?\n", "")
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
