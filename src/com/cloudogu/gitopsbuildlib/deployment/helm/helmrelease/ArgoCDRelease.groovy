package com.cloudogu.gitopsbuildlib.deployment.helm.helmrelease

import com.cloudogu.gitopsbuildlib.docker.DockerWrapper

class ArgoCDRelease extends HelmRelease {

    protected DockerWrapper dockerWrapper

    ArgoCDRelease(def script) {
        super(script)
        dockerWrapper = new DockerWrapper(script)
    }

    @Override
    String create(Map gitopsConfig, String namespace, String mergedValuesFile) {
        Map helmConfig = gitopsConfig.deployments.helm
        String application = gitopsConfig.application

        String helmRelease = ""
        if (helmConfig.repoType == 'GIT') {
            helmRelease = createResourcesFromGitRepo(gitopsConfig, application, mergedValuesFile)
        } else if (helmConfig.repoType == 'HELM') {
            helmRelease = createResourcesFromHelmRepo(gitopsConfig, application, mergedValuesFile)
        }
        return helmRelease
    }

    private String createResourcesFromGitRepo(Map gitopsConfig, String application, String mergedValuesFile) {
        Map helmConfig = gitopsConfig.deployments.helm

        def chartPath = ''
        if (helmConfig.containsKey('chartPath')) {
            chartPath = helmConfig.chartPath
        }

        return createHelmRelease(chartPath as String, application, gitopsConfig.buildImages.helm, mergedValuesFile)
    }

    private String createResourcesFromHelmRepo(Map gitopsConfig, String application, String mergedValuesFile) {
        return createHelmRelease(gitopsConfig.deployments.helm.chartName, application, gitopsConfig.buildImages.helm, mergedValuesFile)
    }

    private String createHelmRelease(def chartPath, String application, def helmImageConfig, String mergedValuesFile) {
        String helmRelease = ""
        dockerWrapper.withDockerImage(helmImageConfig) {
            String templateScript = "helm template ${application} ${script.env.WORKSPACE}/.helmChartTempDir/chart/${chartPath} -f ${mergedValuesFile}"
            helmRelease = script.sh returnStdout: true, script: templateScript
        }

        // this line removes all empty lines since helm template creates some and the helm validator will throw an error if there are emtpy lines present
        helmRelease = helmRelease.replaceAll("(?m)^[ \t]*\r?\n", "")
        return helmRelease
    }
}
