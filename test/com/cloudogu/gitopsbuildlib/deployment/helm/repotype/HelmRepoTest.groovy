package com.cloudogu.gitopsbuildlib.deployment.helm.repotype

import com.cloudogu.gitopsbuildlib.ScriptMock
import org.junit.jupiter.api.*
import static org.assertj.core.api.Assertions.assertThat

class HelmRepoTest {

    def scriptMock = new ScriptMock()
    def helmRepo = new HelmRepo(scriptMock.mock)

    @Test
    void 'merges values successfully'() {
        helmRepo.prepareRepo([
            buildImages: [
                helm: 'helmImage'
            ],
            deployments: [
                helm: [
                    repoUrl: 'url',
                    chartName: 'chartName',
                    version: '1.0'
                    ]
                ]
            ], ".helmChartTempDir", "chartRoot")

        assertThat(scriptMock.actualShArgs[0]).isEqualTo('helm repo add chartRepo url')
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('helm repo update')
        assertThat(scriptMock.actualShArgs[2]).isEqualTo('helm pull chartRepo/chartName --version=1.0 --untar --untardir=workspace/.helmChartTempDir/chartRoot')
    }
}
