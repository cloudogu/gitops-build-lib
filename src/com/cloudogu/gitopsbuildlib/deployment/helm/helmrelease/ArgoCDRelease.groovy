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
            helmRelease = createResourcesFromGitRepo(gitopsConfig, application, namespace, mergedValuesFile)
        } else if (helmConfig.repoType == 'HELM') {
            helmRelease = createResourcesFromHelmRepo(gitopsConfig, application, namespace, mergedValuesFile)
        }
        return helmRelease
    }

    private String createResourcesFromGitRepo(Map gitopsConfig, String application, String namespace, String mergedValuesFile) {
        Map helmConfig = gitopsConfig.deployments.helm

        def chartPath = ''
        if (helmConfig.containsKey('chartPath')) {
            chartPath = helmConfig.chartPath
        }

        return createHelmRelease(gitopsConfig, chartPath as String, application, namespace, gitopsConfig.buildImages.helm, mergedValuesFile)
    }

    private String createResourcesFromHelmRepo(Map gitopsConfig, String application, String namespace, String mergedValuesFile) {
        return createHelmRelease(gitopsConfig, gitopsConfig.deployments.helm.chartName, application, namespace, gitopsConfig.buildImages.helm, mergedValuesFile)
    }

    private String createHelmRelease(Map gitopsConfig, def chartPath, String application, String namespace, def helmImageConfig, String mergedValuesFile) {
        String helmRelease = ""
        dockerWrapper.withDockerImage(helmImageConfig) {
            String templateScript = "helm template ${application} ${script.env.WORKSPACE}/.helmChartTempDir/chart/${chartPath} -n ${namespace} --kube-version ${gitopsConfig.k8sVersion} -f ${mergedValuesFile}"
            helmRelease = script.sh returnStdout: true, script: templateScript
        }

        // this line removes all empty lines since helm template creates some and the helm validator will throw an error if there are emtpy lines present
        helmRelease = helmRelease.replaceAll("(?m)^[ \t]*\r?\n", "")
        return helmRelease
    }
}
