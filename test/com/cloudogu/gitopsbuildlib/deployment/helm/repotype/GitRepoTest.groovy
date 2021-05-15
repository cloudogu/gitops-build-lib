package com.cloudogu.gitopsbuildlib.deployment.helm.repotype

import com.cloudogu.gitopsbuildlib.ScriptMock
import org.junit.jupiter.api.*

import static org.assertj.core.api.Assertions.assertThat

class GitRepoTest {

    def scriptMock = new ScriptMock()
    def gitRepo = new GitRepo(scriptMock.mock)

    @Test
    void 'merges values successfully'() {
        gitRepo.prepareRepo([
            buildImages: [
                helm: 'helmImage'
            ],
            deployments: [
                helm: [
                    repoUrl: 'url',
                    chartPath: 'chartPath',
                    version: '1.0'
                    ]
                ]
            ], ".helmChartTempDir", "chartRootDir")

        assertThat(scriptMock.actualShArgs[0]).isEqualTo('helm dep update workspace/.helmChartTempDir/chartRootDir/chartPath')
    }
}
