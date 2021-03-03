package com.cloudogu.gitopsbuildlib.deployments

import com.cloudogu.gitopsbuildlib.ScriptMock
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class HelmTest {

    def scriptMock = new ScriptMock()
    def dockerMock = scriptMock.dockerMock
    def helm = new Helm(scriptMock.mock)

    @Test
    void 'creating folders for update step'() {

        helm.prepareApplicationFolders(
            'staging',
            [
                application: 'testapp',
                deployments: [
                    sourcePath: 'k8s',
                    helm      : [
                        repoUrl: 'repoUrl'
                    ]
                ]
            ]
        )
        assertThat(dockerMock.actualImages[0]).isEqualTo('ghcr.io/cloudogu/helm:3.4.1-1')
        assertThat(scriptMock.actualShArgs[0]).isEqualTo('mkdir -p staging/testapp/')
        assertThat(scriptMock.actualWriteFileArgs[0]).isEqualTo(
            '[file:staging/testapp/mergedValues.yaml, ' +
                'text:[' +
                'mkdir -p staging/testapp/, ' +
                'git clone repoUrl workspace/chart || true, ' +
                '[returnStdout:true, ' +
                'script:helm values workspace/chart -f workspace/k8s/values-staging.yaml -f workspace/k8s/values-shared.yaml ]]]')
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('git clone repoUrl workspace/chart || true')
        assertThat(scriptMock.actualShArgs[2]).isEqualTo('[returnStdout:true, script:helm values workspace/chart -f workspace/k8s/values-staging.yaml -f workspace/k8s/values-shared.yaml ]')
        assertThat(scriptMock.actualShArgs[3]).isEqualTo('rm -rf workspace/chart || true')
    }

    @Test
    void 'successful update'() {
        helm.update(
            'staging',
            [
                application: 'testApp',
                deployments: [
                    helm: [
                        repoType: 'GIT',
                        repoUrl: 'gitRepo',
                        version: 'version',
                        updateValues  : [[fieldPath: "image.name", newValue: 'imageName']]
                    ]
                ]
            ]
        )
        assertThat(scriptMock.actualShArgs[0]).isEqualTo('rm staging/testApp/mergedValues.yaml')
        assertThat(scriptMock.actualReadYamlArgs[0]).isEqualTo('[file:staging/testApp/mergedValues.yaml]')
        assertThat(scriptMock.actualWriteYamlArgs[0]).isEqualTo('[file:staging/testApp/mergedValues.yaml, data:[image:[name:imageName], spec:[template:[spec:[containers:[[image:oldImageName, name:application]]]]]], overwrite:true]')
        assertThat(scriptMock.actualWriteFileArgs[0]).isEqualTo('[file:staging/testApp/helmRelease.yaml, text:apiVersion: helm.fluxcd.io/v1\n' +
            'kind: HelmRelease\n' +
            'metadata:\n' +
            '  name: testApp\n' +
            '  namespace: fluxv1-staging\n' +
            '  annotations:\n' +
            '    fluxcd.io/automated: "false"\n' +
            'spec:\n' +
            '  releaseName: testApp\n' +
            '  chart:\n' +
            '    git: gitRepo\n' +
            '    ref: version\n' +
            '    path: .\n' +
            '  values:\n' +
            '    ---\n' +
            '    spec:\n' +
            '      template:\n' +
            '        spec:\n' +
            '          containers:\n' +
            '            - name: \'application\'\n' +
            '              image: \'oldImageName\'\n' +
            '    image:\n' +
            '      name: \'oldImageName\'\n' +
            ']')

    }
}
