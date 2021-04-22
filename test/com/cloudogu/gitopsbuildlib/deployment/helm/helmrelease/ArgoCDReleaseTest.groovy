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
            repoType: 'GIT',
            repoUrl: 'url',
            chartName: 'chartName',
            chartPath: 'path',
            version: '1.0'
        ],
            'app',
            'namespace',
            'this/is/a/valuesfile')

        assertThat(scriptMock.actualShArgs[0]).isEqualTo('[returnStdout:true, script:helm template app chart/path -f this/is/a/valuesfile]')
    }

    @Test
    void 'correct helm release with git repo without chartPath'() {
        argoCdReleaseTest.create([
            repoType: 'GIT',
            repoUrl: 'url',
            chartName: 'chartName',
            version: '1.0'
        ],
            'app',
            'namespace',
            'this/is/a/valuesfile')

        assertThat(scriptMock.actualShArgs[0]).isEqualTo('[returnStdout:true, script:helm template app chart/ -f this/is/a/valuesfile]')
    }

    @Test
    void 'correct helm release with helm repo'() {
        argoCdReleaseTest.create([
            repoType: 'HELM',
            repoUrl: 'url',
            chartName: 'chartName',
            version: '1.0'
        ],
            'app',
            'namespace',
            'this/is/a/valuesfile')

        assertThat(scriptMock.actualShArgs[0]).isEqualTo('[returnStdout:true, script:helm template app chart/chartName -f this/is/a/valuesfile]')
    }
}
