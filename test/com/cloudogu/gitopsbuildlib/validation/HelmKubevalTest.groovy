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
            [:],
            [
                deployments:[
                    helm: [
                        repoType: 'GIT',
                        repoUrl: 'chartRepo/namespace/repoPath',
                        chartPath: 'chartPath',
                        version: 'version'
                    ]
                ]
            ]
        )
        assertThat(scriptMock.actualShArgs[0]).isEqualTo('helm kubeval target/chart/chartPath -f target/mergedValues.yaml -v 1.5 --strict --ignore-missing-schemas')
    }

    @Test
    void 'is executed with repoType HELM'() {
        helmKubeval.validate(
            'target',
            [:],
            [
                deployments:[
                    helm: [
                        repoType: 'HELM',
                        chartName: 'chart',
                        repoUrl: 'chartRepo',
                        version: 'version'
                    ]
                ]
            ]
        )
        assertThat(scriptMock.actualShArgs[0]).isEqualTo('helm kubeval target/chart/chart -f target/mergedValues.yaml -v 1.5 --strict --ignore-missing-schemas')
    }
}
