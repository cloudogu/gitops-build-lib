package com.cloudogu.gitopsbuildlib.deployment.helm.helmrelease

import com.cloudogu.gitopsbuildlib.ScriptMock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat

class FluxV1ReleaseTest {

    def scriptMock = new ScriptMock()
    def fluxV1Release = new FluxV1Release(scriptMock.mock)

    @BeforeEach
    void init () {
        scriptMock.configYaml = 'a: b'
    }

    @Test
    void 'correct helm release with git repo'() {
        def output = fluxV1Release.create([
            application: 'app',
            deployments: [
                helm: [
                    repoType: 'GIT',
                    repoUrl: 'url',
                    chartName: 'chartName',
                    version: '1.0'
                    ]
                ]
            ],
            'namespace', 'this/is/a/valuesfile')

        assertThat(output).isEqualTo("""apiVersion: helm.fluxcd.io/v1
kind: HelmRelease
metadata:
  name: app
  namespace: namespace
  annotations:
    fluxcd.io/automated: "false"
spec:
  releaseName: app
  chart:
    git: url
    ref: 1.0
    path: .
  values:
    a: b
""")
    }

    @Test
    void 'correct helm release with helm repo'() {
        def output = fluxV1Release.create([
            application: 'app',
            deployments: [
                helm: [
                    repoType: 'HELM',
                    repoUrl: 'url',
                    chartName: 'chartName',
                    version: '1.0'
                    ]
                ]
            ],
            'namespace',
            'this/is/a/valuesfile')

        assertThat(output).isEqualTo("""apiVersion: helm.fluxcd.io/v1
kind: HelmRelease
metadata:
  name: app
  namespace: namespace
  annotations:
    fluxcd.io/automated: "false"
spec:
  releaseName: app
  chart:
    repository: url
    name: chartName
    version: 1.0
  values:
    a: b
""")
    }
}
