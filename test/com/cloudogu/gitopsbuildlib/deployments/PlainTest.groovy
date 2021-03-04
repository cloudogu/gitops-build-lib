package com.cloudogu.gitopsbuildlib.deployments

import com.cloudogu.gitopsbuildlib.ScriptMock
import org.junit.Test
import static org.assertj.core.api.Assertions.assertThat


class PlainTest {

    def scriptMock = new ScriptMock()
    def plain = new Plain(scriptMock.mock)

    @Test
    void 'creating folders for update step'() {

        plain.prepareApplicationFolders(
            'staging',
            [
                application: 'testapp',
                deployments: [
                    sourcePath: 'k8s'
                ]
            ]
        )
        assertThat(scriptMock.actualEchoArgs[0]).isEqualTo('Copying k8s payload from application repo to gitOps Repo: \'k8s/staging/*\' to \'staging/testapp\'')
        assertThat(scriptMock.actualShArgs[0]).isEqualTo('mkdir -p staging/testapp/')
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('mkdir -p .config/')
        assertThat(scriptMock.actualShArgs[2]).isEqualTo('cp workspace/k8s/staging/* staging/testapp/ || true')
        assertThat(scriptMock.actualShArgs[3]).isEqualTo('cp workspace/*.yamllint.yaml .config/ || true')
    }

    @Test
    void 'successful update'() {

        plain.update(
            'staging',
            [
                application: 'testApp',
                deployments: [
                    plain: [
                        updateImages: [
                            [filename     : "deployment.yaml", // relative to deployments.path
                             containerName: 'application',
                             imageName    : 'imageNameReplacedTest']
                        ]
                    ]
                ]
            ]
        )
        assertThat(scriptMock.actualReadYamlArgs[0]).isEqualTo('[file:staging/testApp/deployment.yaml]')
        assertThat(scriptMock.actualWriteYamlArgs[0]).isEqualTo('[file:staging/testApp/deployment.yaml, data:[spec:[template:[spec:[containers:[[image:imageNameReplacedTest, name:application]]]]], to:[be:[changed:oldValue]]], overwrite:true]')

    }
}
