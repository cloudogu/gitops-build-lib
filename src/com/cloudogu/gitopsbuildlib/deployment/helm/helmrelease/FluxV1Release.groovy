package com.cloudogu.gitopsbuildlib.deployment.helm.helmrelease

class FluxV1Release extends HelmRelease {

    FluxV1Release(def script) {
        super(script)
    }

    @Override
    String create(Map gitopsConfig, String namespace, String mergedValuesFile) {
        Map helmConfig = gitopsConfig.deployments.helm
        String application = gitopsConfig.application

        def values = fileToInlineYaml(mergedValuesFile)
        def chart = getChart(helmConfig)
        return """apiVersion: helm.fluxcd.io/v1
kind: HelmRelease
metadata:
  name: ${application}
  namespace: ${namespace}
  annotations:
    fluxcd.io/automated: "false"
spec:
  releaseName: ${application}
  chart:${chart}
  values:
${values}
"""
    }

    private String gitRepoChart(Map helmConfig) {
        def chartPath = "."
        if (helmConfig.containsKey('chartPath') && helmConfig.chartPath) {
            chartPath = helmConfig.chartPath
        }

        return """
    git: ${helmConfig.repoUrl}
    ref: ${helmConfig.version}
    path: ${chartPath}"""
    }

    private String helmRepoChart(Map helmConfig) {
        return """
    repository: ${helmConfig.repoUrl}
    name: ${helmConfig.chartName}
    version: ${helmConfig.version}"""
    }

    private String getChart(Map helmConfig) {
        if (helmConfig.repoType == 'GIT') {
            return gitRepoChart(helmConfig)
        } else if (helmConfig.repoType == 'HELM') {
            return helmRepoChart(helmConfig)
        } else if (helmConfig.repoType == 'LOCAL') {
            return script.error("Helm repoType LOCAL not supported for fluxv1")
        }
        return null // Validated in base class Helm
    }
}
