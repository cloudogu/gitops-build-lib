package com.cloudogu.gitopsbuildlib.deployment.repotype

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
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('rm -rf workspace/chart || true')
        assertThat(scriptMock.actualGitArgs[0]).isEqualTo('[url:url, branch:main, changelog:false, poll:false]')
    }

    @Test
    void 'create helm release yields correct helmRelease'() {
        def helmRelease = gitRepo.createHelmRelease([
            repoUrl: 'url',
            chartName: 'chartName',
            version: '1.0'
        ],
            'app',
            'namespace',
            'valuesFile')

        assertThat(helmRelease).isEqualTo('''\
apiVersion: helm.fluxcd.io/v1
kind: HelmRelease
metadata:
  name: app
  namespace: namespace
  annotations:
    fluxcd.io/automated: "false"
spec:
  releaseName: app
  chart:
    git: url
    ref: 1.0
    path: .
  values:
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

}
