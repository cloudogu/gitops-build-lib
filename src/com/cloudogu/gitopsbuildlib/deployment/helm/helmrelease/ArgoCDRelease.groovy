package com.cloudogu.gitopsbuildlib.deployment.helm.helmrelease

import com.cloudogu.gitopsbuildlib.docker.DockerWrapper

class ArgoCDRelease extends HelmRelease{

    protected DockerWrapper dockerWrapper

    ArgoCDRelease(def script) {
        super(script)
        dockerWrapper = new DockerWrapper(script)
    }

    @Override
    String create(Map helmConfig, String application, String namespace, String mergedValuesFile) {

        String helmRelease = ""
        if (helmConfig.repoType == 'GIT') {
            helmRelease = createResourcesFromGitRepo(helmConfig, application, mergedValuesFile)
        } else if (helmConfig.repoType == 'HELM') {
            helmRelease = createResourcesFromHelmRepo(helmConfig, application, mergedValuesFile)
        }
        return helmRelease
    }

    private String createResourcesFromGitRepo(Map helmConfig, String application, String mergedValuesFile) {

        def chartPath = ''
        if (helmConfig.containsKey('chartPath')) {
            chartPath = helmConfig.chartPath
        }

        return createHelmRelease(chartPath as String, application, mergedValuesFile)
    }

    private String createResourcesFromHelmRepo(Map helmConfig, String application, String mergedValuesFile) {
        return createHelmRelease(helmConfig.chartName as String, application, mergedValuesFile)
    }

    private String createHelmRelease(String chartPath, String application, String mergedValuesFile) {
        String helmRelease = ""
        dockerWrapper.withHelm {
            String templateScript = "helm template ${application} ${script.env.WORKSPACE}/.helmChartTempDir/chart/${chartPath} -f ${mergedValuesFile}"
            helmRelease = script.sh returnStdout: true, script: templateScript
        }

        // this line removes all empty lines since helm template creates some and the helm validator will throw an error if there are emtpy lines present
        helmRelease = helmRelease.replaceAll("(?m)^[ \t]*\r?\n", "")
        return helmRelease
    }
}
