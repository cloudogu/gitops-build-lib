package com.cloudogu.gitopsbuildlib.deployment

import com.cloudogu.gitopsbuildlib.ScriptMock
import com.cloudogu.gitopsbuildlib.deployment.plain.Plain
import org.junit.jupiter.api.*
import static org.assertj.core.api.Assertions.assertThat


class PlainTest {

    def scriptMock = new ScriptMock()
    def plain = new Plain(scriptMock.mock, [
        application: 'testApp',
        deployments: [
            sourcePath: 'k8s',
            plain: [
                updateImages: [
                    [filename     : "deployment.yaml", // relative to deployments.path
                     containerName: 'application',
                     imageName    : 'imageNameReplacedTest']
                ]
            ]
        ]
    ])

    @Test
    void 'successful update'() {

        plain.postValidation('staging')
        assertThat(scriptMock.actualReadYamlArgs[0]).isEqualTo('[file:staging/testApp/k8s/deployment.yaml]')
        assertThat(scriptMock.actualWriteYamlArgs[0]).isEqualTo('[file:staging/testApp/k8s/deployment.yaml, data:[spec:[template:[spec:[containers:[[image:imageNameReplacedTest, name:application]]]]], to:[be:[changed:oldValue]]], overwrite:true]')

    }
}
