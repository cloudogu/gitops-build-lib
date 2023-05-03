package com.cloudogu.gitopsbuildlib.deployment.helm.helmrelease

import com.cloudogu.gitopsbuildlib.ScriptMock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat

class HelmReleaseTest {

    def scriptMock = new ScriptMock()
    def repoType = new HelmReleaseUnderTest(scriptMock.mock)

    @BeforeEach
    void init () {
        scriptMock.configYaml = 'a: b'
    }
    
    @Test
    void 'inline yaml test'() {
        def output = repoType.fileToInlineYaml('filepath')
        assertThat(scriptMock.actualReadFileArgs[0]).isEqualTo('filepath')
        assertThat(output).isEqualTo('    a: b')
    }

    class HelmReleaseUnderTest extends HelmRelease {

        HelmReleaseUnderTest(Object script) {
            super(script)
        }

        @Override
        String create(Map gitopsConfig, String namespace, String valuesFile) {
            return null
        }
    }
}
