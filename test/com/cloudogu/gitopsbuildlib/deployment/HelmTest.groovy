package com.cloudogu.gitopsbuildlib.deployment

import com.cloudogu.gitopsbuildlib.ScriptMock
import com.cloudogu.gitopsbuildlib.deployment.helm.Helm
import org.junit.jupiter.api.*

import static org.assertj.core.api.Assertions.assertThat

class HelmTest {

    def scriptMock = new ScriptMock()
    def dockerMock = scriptMock.dockerMock
    def helmGit = new Helm(scriptMock.mock, [
        application: 'testapp',
        gitopsTool: 'FLUX',
        stages: [
            staging: [
                namespace: 'fluxv1-staging'
            ]
        ],
        deployments: [
            sourcePath: 'k8s',
            helm      : [
                repoType: 'GIT',
                repoUrl: 'repoUrl',
                chartPath: 'chartPath'
            ]
        ],
        fileConfigmaps: [
            [
                name : "index",
                sourceFilePath : "../index.html",
                stage: ["staging"]
            ]
        ]
    ])
    def helmHelm = new Helm(scriptMock.mock, [
        application: 'testapp',
        gitopsTool: 'FLUX',
        stages: [
            staging: [
                namespace: 'fluxv1-staging'
            ]
        ],
        deployments: [
            sourcePath: 'k8s',
            helm      : [
                repoType: 'HELM',
                repoUrl: 'repoUrl',
                chartName: 'chartName',
                credentials: 'creds',
                version: '1.0'
            ]
        ],
        fileConfigmaps: [
            [
                name : "index",
                sourceFilePath : "../index.html",
                stage: ["staging"]
            ]
        ]
    ])

    @Test
    void 'creating helm release with git repo'() {
        helmGit.preValidation('staging')

        assertThat(dockerMock.actualImages[0]).contains('ghcr.io/cloudogu/helm:')
        assertThat(scriptMock.actualShArgs[0]).isEqualTo('helm dep update chart/chartPath')
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('[returnStdout:true, script:helm values chart/chartPath -f workspace/k8s/values-staging.yaml -f workspace/k8s/values-shared.yaml ]')
        assertThat(scriptMock.actualShArgs[2]).isEqualTo('rm staging/testapp/mergedValues.yaml')
        assertThat(scriptMock.actualWriteFileArgs[0]).isEqualTo('[file:staging/testapp/mergedValues.yaml, text:[helm dep update chart/chartPath, [returnStdout:true, script:helm values chart/chartPath -f workspace/k8s/values-staging.yaml -f workspace/k8s/values-shared.yaml ]]]')
        assertThat(scriptMock.actualWriteFileArgs[1]).isEqualTo('''[file:staging/testapp/applicationRelease.yaml, text:apiVersion: helm.fluxcd.io/v1
kind: HelmRelease
metadata:
  name: testapp
  namespace: fluxv1-staging
  annotations:
    fluxcd.io/automated: "false"
spec:
  releaseName: testapp
  chart:
    git: repoUrl
    ref: null
    path: chartPath
  values:
    ---
    #this part is only for PlainTest regarding updating the image name
    spec:
      template:
        spec:
          containers:
            - name: \'application\'
              image: \'oldImageName\'
    #this part is only for HelmTest regarding changing the yaml values
    to:
      be:
        changed: \'oldValue\'
]''')
    }

    @Test
    void 'creating helm release with helm repo'() {
        helmHelm.preValidation('staging')

        assertThat(dockerMock.actualImages[0]).contains('ghcr.io/cloudogu/helm:')
        assertThat(scriptMock.actualShArgs[0]).isEqualTo('helm repo add chartRepo repoUrl')
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('helm repo update')
        assertThat(scriptMock.actualShArgs[2]).isEqualTo('helm pull chartRepo/chartName --version=1.0 --untar --untardir=workspace/chart')
        assertThat(scriptMock.actualShArgs[3]).isEqualTo('[returnStdout:true, script:helm values workspace/chart/chartName -f workspace/k8s/values-staging.yaml -f workspace/k8s/values-shared.yaml ]')
        assertThat(scriptMock.actualShArgs[4]).isEqualTo('rm -rf workspace/chart || true')
        assertThat(scriptMock.actualShArgs[5]).isEqualTo('rm staging/testapp/mergedValues.yaml')
        assertThat(scriptMock.actualWriteFileArgs[0]).isEqualTo('[file:staging/testapp/mergedValues.yaml, ' +
            'text:[helm repo add chartRepo repoUrl, helm repo update, ' +
            'helm pull chartRepo/chartName --version=1.0 --untar --untardir=workspace/chart, ' +
            '[returnStdout:true, script:helm values workspace/chart/chartName -f workspace/k8s/values-staging.yaml -f workspace/k8s/values-shared.yaml ]]]')
        assertThat(scriptMock.actualWriteFileArgs[1]).isEqualTo('''[file:staging/testapp/applicationRelease.yaml, text:apiVersion: helm.fluxcd.io/v1
kind: HelmRelease
metadata:
  name: testapp
  namespace: fluxv1-staging
  annotations:
    fluxcd.io/automated: "false"
spec:
  releaseName: testapp
  chart:
    repository: repoUrl
    name: chartName
    version: 1.0
  values:
    ---
    #this part is only for PlainTest regarding updating the image name
    spec:
      template:
        spec:
          containers:
            - name: \'application\'
              image: \'oldImageName\'
    #this part is only for HelmTest regarding changing the yaml values
    to:
      be:
        changed: \'oldValue\'
]''')
    }
}
