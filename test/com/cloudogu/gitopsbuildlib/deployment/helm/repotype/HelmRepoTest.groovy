package com.cloudogu.gitopsbuildlib.deployment.helm.repotype

import com.cloudogu.gitopsbuildlib.ScriptMock
import org.junit.jupiter.api.*
import static org.assertj.core.api.Assertions.assertThat

class HelmRepoTest {

    def scriptMock = new ScriptMock()
    def helmRepo = new HelmRepo(scriptMock.mock)

    @Test
    void 'merges values successfully'() {
        helmRepo.mergeValues([
            repoUrl: 'url',
            chartName: 'chartName',
            version: '1.0'
        ], [
            'file1',
            'file2'
        ] as String[])

        assertThat(scriptMock.actualShArgs[0]).isEqualTo('helm repo add chartRepo url')
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('helm repo update')
        assertThat(scriptMock.actualShArgs[2]).isEqualTo('helm pull chartRepo/chartName --version=1.0 --untar --untardir=workspace/chart')
        assertThat(scriptMock.actualShArgs[3]).isEqualTo('[returnStdout:true, script:helm values workspace/chart/chartName -f file1 -f file2 ]')
        assertThat(scriptMock.actualShArgs[4]).isEqualTo('rm -rf workspace/chart || true')
    }

    //TODO implement in fluxv1helmrepo class
//    @Test
//    void 'create helm release yields correct helmRelease'() {
//        def helmRelease = helmRepo.create([
//            repoUrl: 'url',
//            chartName: 'chartName',
//            version: '1.0'
//        ],
//        'app',
//        'namespace',
//        'valuesFile')
//
//        assertThat(helmRelease).isEqualTo('''\
//apiVersion: helm.fluxcd.io/v1
//kind: HelmRelease
//metadata:
//  name: app
//  namespace: namespace
//  annotations:
//    fluxcd.io/automated: "false"
//spec:
//  releaseName: app
//  chart:
//    repository: url
//    name: chartName
//    version: 1.0
//  values:
//    ---
//    #this part is only for PlainTest regarding updating the image name
//    spec:
//      template:
//        spec:
//          containers:
//            - name: 'application\'
//              image: 'oldImageName'
//    #this part is only for HelmTest regarding changing the yaml values
//    to:
//      be:
//        changed: 'oldValue'
//''')
//    }

}
