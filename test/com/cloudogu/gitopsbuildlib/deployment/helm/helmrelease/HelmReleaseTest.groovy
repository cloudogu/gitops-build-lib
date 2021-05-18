package com.cloudogu.gitopsbuildlib.deployment.helm.helmrelease

import com.cloudogu.gitopsbuildlib.ScriptMock
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat

class HelmReleaseTest {

    def scriptMock = new ScriptMock()
    def repoType = new HelmReleaseUnderTest(scriptMock.mock)

    @Test
    void 'inline yaml test'() {
        def output = repoType.fileToInlineYaml('filepath')
        assertThat(scriptMock.actualReadFileArgs[0]).isEqualTo('filepath')
        assertThat(output).isEqualTo('''\
    ---
    #this part is only for PlainTest regarding updating the image name
    spec:
      template:
        spec:
          containers:
            - name: 'application\'
              image: 'oldImageName'
    #this part is only for HelmTest regarding changing the yaml values
    to:
      be:
        changed: 'oldValue\'''')
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
