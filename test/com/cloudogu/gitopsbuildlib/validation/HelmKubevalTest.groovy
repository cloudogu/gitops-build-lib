package com.cloudogu.gitopsbuildlib.validation

import com.cloudogu.gitopsbuildlib.ScriptMock
import com.cloudogu.gitopsbuildlib.validation.HelmKubeval
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat

class HelmKubevalTest {
    def scriptMock = new ScriptMock()
    def dockerMock = scriptMock.dockerMock
    def gitMock = scriptMock.gitMock
    def helmKubeval = new HelmKubeval(scriptMock.mock)

    @Test
    void 'is executed with repoType GIT'() {
        helmKubeval.validate(
            'target',
            [
                image           : 'img',
                k8sSchemaVersion: '1.5'
            ],
            [
                helm: [
                    repoType: 'GIT',
                    repoUrl: 'chartRepo/namespace/repoPath',
                    chartPath: 'chartPath',
                    version: 'version'
                ]
            ]
        )
        assertThat(dockerMock.actualImages[0]).isEqualTo('img')
        assertThat(scriptMock.actualShArgs[0]).isEqualTo('helm kubeval chart/chartPath -v 1.5')
    }

    @Test
    void 'is executed with repoType HELM'() {
        helmKubeval.validate(
            'target',
            [
                image           : 'img',
                k8sSchemaVersion: '1.5'
            ],
            [
                helm: [
                    repoType: 'HELM',
                    chartName: 'chart',
                    repoUrl: 'chartRepo',
                    version: 'version'
                ]
            ]
        )
        assertThat(dockerMock.actualImages[0]).isEqualTo('img')
        assertThat(scriptMock.actualShArgs[0]).isEqualTo('helm kubeval chart/chart -v 1.5')
    }

    @Test
    void 'is not executed on plain deployment'() {
        helmKubeval.validate(
            'target',
            [image           : 'img',
             k8sSchemaVersion: '1.5'],
            [plain: []]
        )
        assertThat(dockerMock.actualImages[0]).isEqualTo(null)
        assertThat(scriptMock.actualShArgs[0]).isEqualTo(null)
    }
}
