package com.cloudogu.gitopsbuildlib.deployment.helm.helmrelease

class FluxV1GitRepoRelease extends HelmRelease{

    FluxV1GitRepoRelease(def script) {
        super(script)
    }

    @Override
    String create(Map helmConfig, String application, String namespace, String valuesFile) {
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
