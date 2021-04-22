package com.cloudogu.gitopsbuildlib.deployment.helm.repotype;

import com.cloudogu.gitopsbuildlib.ScriptMock;
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat;

class RepoTypeTest {

    def scriptMock = new ScriptMock()
    def repoType = new RepoTypeUnderTest(scriptMock.mock)

    @Test
    void 'values files getting parameters attached'() {
        def output = repoType.valuesFilesWithParameter(['file1.yaml', 'file2.yaml'] as String[])
        assertThat(output).isEqualTo('-f file1.yaml -f file2.yaml ')
    }

    class RepoTypeUnderTest extends RepoType {

        RepoTypeUnderTest(Object script) {
            super(script)
        }

        @Override
        Object mergeValues(Map helmConfig, String[] files) {
            return null;
        }
    }
}
