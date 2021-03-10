package com.cloudogu.gitopsbuildlib.deployment.repotype

import com.cloudogu.gitopsbuildlib.ScriptMock
import org.junit.Test
import static org.assertj.core.api.Assertions.assertThat

class RepoTypeTest {

    def scriptMock = new ScriptMock()
    def repoType = new RepoTypeUnderTest(scriptMock.mock)

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
        changed: 'oldValue'
''')
    }

    class RepoTypeUnderTest extends RepoType {

        RepoTypeUnderTest(Object script) {
            super(script)
        }

        @Override
        def createHelmRelease(Map helmConfig, String application, String namespace, String valuesFile) {
            return null
        }

        @Override
        def mergeValues(Map helmConfig, String[] files) {
            return null
        }
    }
}
