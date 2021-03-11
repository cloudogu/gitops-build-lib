package com.cloudogu.gitopsbuildlib.deployment

import com.cloudogu.gitopsbuildlib.ScriptMock
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class HelmTest {

    def scriptMock = new ScriptMock()
    def dockerMock = scriptMock.dockerMock
    def helmGit = new Helm(scriptMock.mock, [
        application: 'testapp',
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
                sourceFilePath : "../index.html", // relative to deployments.sourcePath
                // additional feature in the future. current default folder is '../generated-resources'
                // destinationFilePath: "../generated-resources/html/" // realtive to deployments.sourcePath
                stage: ["staging"]
            ]
        ]
    ])
    def helmHelm = new Helm(scriptMock.mock, [
        application: 'testapp',
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
                version: '1.0'
            ]
        ],
        fileConfigmaps: [
            [
                name : "index",
                sourceFilePath : "../index.html", // relative to deployments.sourcePath
                // additional feature in the future. current default folder is '../generated-resources'
                // destinationFilePath: "../generated-resources/html/" // realtive to deployments.sourcePath
                stage: ["staging"]
            ]
        ]
    ])

    @Test
    void 'creating helm release with git repo'() {
        helmGit.createPreValidation('staging')

        assertThat(dockerMock.actualImages[0]).isEqualTo('ghcr.io/cloudogu/helm:3.4.1-1')
        assertThat(scriptMock.actualShArgs[0]).isEqualTo('git clone repoUrl workspace/chart || true')
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('[returnStdout:true, script:helm values workspace/chart/chartPath -f workspace/k8s/values-staging.yaml -f workspace/k8s/values-shared.yaml ]')
        assertThat(scriptMock.actualShArgs[2]).isEqualTo('rm -rf workspace/chart || true')
        assertThat(scriptMock.actualShArgs[3]).isEqualTo('rm staging/testapp/mergedValues.yaml')
        assertThat(scriptMock.actualWriteFileArgs[0]).isEqualTo('[file:staging/testapp/mergedValues.yaml, ' +
            'text:[git clone repoUrl workspace/chart || true, ' +
            '[returnStdout:true, ' +
            'script:helm values workspace/chart/chartPath -f workspace/k8s/values-staging.yaml -f workspace/k8s/values-shared.yaml ]]]')
        assertThat(scriptMock.actualWriteFileArgs[1]).isEqualTo('[file:staging/testapp/helmRelease.yaml, text:apiVersion: helm.fluxcd.io/v1\n' +
            'kind: HelmRelease\n' +
            'metadata:\n' +
            '  name: testapp\n' +
            '  namespace: fluxv1-staging\n' +
            '  annotations:\n' +
            '    fluxcd.io/automated: "false"\n' +
            'spec:\n' +
            '  releaseName: testapp\n' +
            '  chart:\n' +
            '    git: repoUrl\n' +
            '    ref: null\n' +
            '    path: .\n' +
            '  values:\n' +
            '    ---\n' +
            '    #this part is only for PlainTest regarding updating the image name\n' +
            '    spec:\n' +
            '      template:\n' +
            '        spec:\n' +
            '          containers:\n' +
            '            - name: \'application\'\n' +
            '              image: \'oldImageName\'\n' +
            '    #this part is only for HelmTest regarding changing the yaml values\n' +
            '    to:\n' +
            '      be:\n' +
            '        changed: \'oldValue\'\n' +
            ']')
    }

    @Test
    void 'creating helm release with helm repo'() {
        helmHelm.createPreValidation('staging')

        assertThat(dockerMock.actualImages[0]).isEqualTo('ghcr.io/cloudogu/helm:3.4.1-1')
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
        assertThat(scriptMock.actualWriteFileArgs[1]).isEqualTo('[file:staging/testapp/helmRelease.yaml, text:apiVersion: helm.fluxcd.io/v1\n' +
            'kind: HelmRelease\n' +
            'metadata:\n' +
            '  name: testapp\n' +
            '  namespace: fluxv1-staging\n' +
            '  annotations:\n' +
            '    fluxcd.io/automated: "false"\n' +
            'spec:\n' +
            '  releaseName: testapp\n' +
            '  chart:\n' +
            '    repository: repoUrl\n' +
            '    name: chartName\n' +
            '    version: 1.0\n' +
            '  values:\n' +
            '    ---\n' +
            '    #this part is only for PlainTest regarding updating the image name\n' +
            '    spec:\n' +
            '      template:\n' +
            '        spec:\n' +
            '          containers:\n' +
            '            - name: \'application\'\n' +
            '              image: \'oldImageName\'\n' +
            '    #this part is only for HelmTest regarding changing the yaml values\n' +
            '    to:\n' +
            '      be:\n' +
            '        changed: \'oldValue\'\n' +
            ']')
    }
}
