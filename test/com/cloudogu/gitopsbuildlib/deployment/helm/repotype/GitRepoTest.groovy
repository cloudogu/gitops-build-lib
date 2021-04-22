package com.cloudogu.gitopsbuildlib.deployment.helm.repotype

import com.cloudogu.gitopsbuildlib.ScriptMock
import org.junit.jupiter.api.*

import static org.assertj.core.api.Assertions.assertThat

class GitRepoTest {

    def scriptMock = new ScriptMock()
    def gitRepo = new GitRepo(scriptMock.mock)

    @Test
    void 'merges values successfully'() {
        gitRepo.mergeValues([
            repoUrl: 'url',
            chartPath: 'chartPath',
            version: '1.0'
        ], [
            'file1',
            'file2'
        ] as String[])

        assertThat(scriptMock.actualShArgs[0]).isEqualTo('[returnStdout:true, script:helm values workspace/chart/chartPath -f file1 -f file2 ]')
    }
}
