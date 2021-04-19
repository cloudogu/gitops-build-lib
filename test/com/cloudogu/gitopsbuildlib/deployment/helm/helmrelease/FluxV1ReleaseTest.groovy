package com.cloudogu.gitopsbuildlib.deployment.helm.helmrelease

import com.cloudogu.gitopsbuildlib.ScriptMock
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat

class FluxV1ReleaseTest {

    def scriptMock = new ScriptMock()
    def fluxV1Release = new FluxV1Release(scriptMock.mock)

    @Test
    void 'correct helm release with git repo'() {
        def output = fluxV1Release.create([
            repoType: 'GIT',
            repoUrl: 'url',
            chartName: 'chartName',
            version: '1.0'
        ],
        'app',
        'namespace',
        'this/is/a/valusfile')

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
    ---
    #this part is only for PlainTest regarding updating the image name
    spec:
      template:
        spec:
          containers:
            - name: 'application'
              image: 'oldImageName'
    #this part is only for HelmTest regarding changing the yaml values
    to:
      be:
        changed: 'oldValue'
""")
    }

    @Test
    void 'correct helm release with helm repo'() {
        def output = fluxV1Release.create([
            repoType: 'HELM',
            repoUrl: 'url',
            chartName: 'chartName',
            version: '1.0'
        ],
            'app',
            'namespace',
            'this/is/a/valusfile')

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
    ---
    #this part is only for PlainTest regarding updating the image name
    spec:
      template:
        spec:
          containers:
            - name: 'application'
              image: 'oldImageName'
    #this part is only for HelmTest regarding changing the yaml values
    to:
      be:
        changed: 'oldValue'
""")
    }
}
