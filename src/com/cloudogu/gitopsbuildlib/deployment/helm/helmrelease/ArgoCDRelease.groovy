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

//        String mergedValuesFileLocation = "${script.env.WORKSPACE}/.configRepoTempDir/${mergedValuesFile}"
        String helmRelease = ""
        if (helmConfig.repoType == 'GIT') {
            helmRelease = createResourcesFromGitRepo(helmConfig, application, mergedValuesFile)
        } else if (helmConfig.repoType == 'HELM') {
            // TODO not yet implemented
        }
        return helmRelease
    }

    private String createResourcesFromGitRepo(Map helmConfig, String application, String mergedValuesFile) {
        String helmRelease = ""

        def chartPath = ''
        if (helmConfig.containsKey('chartPath')) {
            chartPath = helmConfig.chartPath
        }

        dockerWrapper.withHelm {
            script.sh "helm dep update ./chart/${chartPath}"
            String templateScript = "helm template ${application} ./chart/${chartPath} -f ${mergedValuesFile}"
            helmRelease = script.sh returnStdout: true, script: templateScript
        }
        // this line removes all empty lines since helm template creates some and the helm validator will throw an error if there are emtpy lines present
        helmRelease = helmRelease.replaceAll("(?m)^[ \t]*\r?\n", "")
        return helmRelease
    }
}
