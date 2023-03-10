package com.cloudogu.gitopsbuildlib.deployment.helm.repotype

import com.cloudogu.gitopsbuildlib.ScriptMock
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor

import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify 

class GitRepoTest {

    def scriptMock = new ScriptMock()
    def gitRepo = new GitRepo(scriptMock.mock)

    @Test
    void 'merges values successfully'() {
        gitRepo.prepareRepo([
            buildImages: [
                helm: [
                    image: 'helmImage'
                ]
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
        
        ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class)
        verify(scriptMock.gitMock).call(argumentCaptor.capture())
        assertThat(argumentCaptor.getValue().url).isEqualTo('url')
        assertThat(argumentCaptor.getValue().branch).isEqualTo('main')
        verify(scriptMock.gitMock, times(1)).fetch()
    }
    
    @Test
    void 'Respects different main branch of helm repo'() {
        gitRepo.prepareRepo([
            buildImages: [
                helm: [
                    image: 'helmImage'
                ]
            ],
            deployments: [
                helm: [
                    repoUrl: 'url',
                    chartPath: 'chartPath',
                    version: '1.0',
                    mainBranch: 'other'
                    ]
                ]
            ], ".helmChartTempDir", "chartRootDir")

        ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class)
        verify(scriptMock.gitMock).call(argumentCaptor.capture())
        assertThat(argumentCaptor.getValue().url).isEqualTo('url')
        assertThat(argumentCaptor.getValue().branch).isEqualTo('other')
        verify(scriptMock.gitMock, times(1)).fetch()
    }
}
