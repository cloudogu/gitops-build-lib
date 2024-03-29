package com.cloudogu.gitopsbuildlib.deployment.helm.helmrelease

import com.cloudogu.gitopsbuildlib.ScriptMock
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat

class ArgoCDReleaseTest {

    def scriptMock = new ScriptMock()
    def argoCdReleaseTest = new ArgoCDRelease(scriptMock.mock)

    @Test
    void 'correct helm release with git repo and chartPath'() {
        argoCdReleaseTest.create([
            k8sVersion: '1.24.8',
            application: 'app',
            deployments: [
                helm: [
                    repoType : 'GIT',
                    repoUrl  : 'url',
                    chartName: 'chartName',
                    chartPath: 'path',
                    version  : '1.0'
                    ]
                ],
                buildImages: [
                    helm: [
                        image: 'helmImg'
                    ]
                ]
            ],
            'namespace',
            'this/is/a/valuesfile')

        assertThat(scriptMock.dockerMock.actualImages[0]).isEqualTo('helmImg')
        assertThat(scriptMock.actualShArgs[0]).isEqualTo('[returnStdout:true, script:helm template app workspace/.helmChartTempDir/chart/path -n namespace --kube-version 1.24.8 -f this/is/a/valuesfile]')
    }

    @Test
    void 'correct helm release with git repo without chartPath'() {
        argoCdReleaseTest.create([
            k8sVersion: '1.24.8',
            application: 'app',
            deployments: [
                helm: [
                    repoType : 'GIT',
                    repoUrl  : 'url',
                    chartName: 'chartName',
                    version  : '1.0'
                    ]
                ],
                buildImages: [
                    helm: [
                        image: 'helmImg'
                    ]
                ]
            ],
            'namespace',
            'this/is/a/valuesfile')

        assertThat(scriptMock.dockerMock.actualImages[0]).isEqualTo('helmImg')
        assertThat(scriptMock.actualShArgs[0]).isEqualTo('[returnStdout:true, script:helm template app workspace/.helmChartTempDir/chart/ -n namespace --kube-version 1.24.8 -f this/is/a/valuesfile]')
    }

    @Test
    void 'correct helm release with helm repo'() {
        argoCdReleaseTest.create([
            k8sVersion: '1.24.8',
            application: 'app',
            deployments: [
                helm: [
                    repoType : 'HELM',
                    repoUrl  : 'url',
                    chartName: 'chartName',
                    version  : '1.0'
                    ]
                ],
                buildImages: [
                    helm: [
                        image: 'helmImg'
                    ]
                ]
            ],
            'namespace',
            'this/is/a/valuesfile')

        assertThat(scriptMock.dockerMock.actualImages[0]).isEqualTo('helmImg')
        assertThat(scriptMock.actualShArgs[0]).isEqualTo('[returnStdout:true, script:helm template app workspace/.helmChartTempDir/chart/chartName -n namespace --kube-version 1.24.8 -f this/is/a/valuesfile]')
    }
    
    @Test
    void 'correct helm release with local repo'() {
        argoCdReleaseTest.create([
            k8sVersion: '1.24.8',
            application: 'app',
            deployments: [
                helm: [
                    repoType : 'LOCAL',
                    chartPath: 'my/path',
                    version  : '1.0'
                    ]
                ],
                buildImages: [
                    helm: [
                        image: 'helmImg'
                    ]
                ]
            ],
            'namespace',
            'this/is/a/valuesfile')

        assertThat(scriptMock.dockerMock.actualImages[0]).isEqualTo('helmImg')
        assertThat(scriptMock.actualShArgs[0]).isEqualTo('[returnStdout:true, ' +
            'script:helm template app workspace/my/path -n namespace --kube-version 1.24.8 -f this/is/a/valuesfile]')
    }
}
