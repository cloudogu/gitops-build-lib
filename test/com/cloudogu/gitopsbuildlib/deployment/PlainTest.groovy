package com.cloudogu.gitopsbuildlib.deployment

import com.cloudogu.gitopsbuildlib.ScriptMock
import com.cloudogu.gitopsbuildlib.deployment.plain.Plain
import com.cloudogu.gitopsbuildlib.validation.HelmKubeval
import com.cloudogu.gitopsbuildlib.validation.Kubeval
import com.cloudogu.gitopsbuildlib.validation.Yamllint
import org.junit.jupiter.api.*
import static org.assertj.core.api.Assertions.assertThat


class PlainTest {

    def scriptMock = new ScriptMock()
    def plain = new Plain(scriptMock.mock, [
        application: 'app',
        gitopsTool: 'FLUX',
        deployments: [
            sourcePath: 'k8s',
            plain: [
                updateImages: [
                    [filename     : "deployment.yaml", // relative to deployments.path
                     containerName: 'application',
                     imageName    : 'imageNameReplacedTest']
                ]
            ]
        ],
        validators: [
            yamllint: [
                validator: new Yamllint(scriptMock.mock),
                enabled: true,
                config: [
                    image: 'img'
                ]
            ],
            kubeval: [
                validator: new Kubeval(scriptMock.mock),
                enabled: true,
                config: [
                    image: 'img'
                ]
            ],
            helmKubeval: [
                validator: new HelmKubeval(scriptMock.mock),
                enabled: true,
                config: [
                    image: 'img'
                ]
            ]
        ],
    ])

    @Test
    void 'successful update'() {

        plain.preValidation('staging')
        assertThat(scriptMock.actualReadYamlArgs[0]).isEqualTo('[file:staging/app/deployment.yaml]')
        assertThat(scriptMock.actualWriteYamlArgs[0]).isEqualTo('[file:staging/app/deployment.yaml, data:[spec:[template:[spec:[containers:[[image:imageNameReplacedTest, name:application]]]]], to:[be:[changed:oldValue]]], overwrite:true]')
    }

    @Test
    void 'flux plain validates with yamllint and kubeval'() {
        plain.validate('staging')

        assertThat(scriptMock.actualEchoArgs[0]).isEqualTo('Starting validator Yamllint for FLUX in PLAIN resources')
        assertThat(scriptMock.actualEchoArgs[1]).isEqualTo('Starting validator Kubeval for FLUX in PLAIN resources')
        assertThat(scriptMock.actualEchoArgs[2]).isEqualTo('Skipping validator HelmKubeval because it is configured as enabled=false or doesn\'t support the given gitopsTool=flux or sourceType=plain')

        assertThat(scriptMock.actualShArgs[0]).isEqualTo('yamllint -f standard staging/app')
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('kubeval -d staging/app -v null --strict --ignore-missing-schemas')
    }

    @Test
    void 'argo plain validates with yamllint and kubeval'() {
        plain.gitopsConfig['gitopsTool'] = 'ARGO'
        plain.validate('staging')

        assertThat(scriptMock.actualEchoArgs[0]).isEqualTo('Starting validator Yamllint for ARGO in PLAIN resources')
        assertThat(scriptMock.actualEchoArgs[1]).isEqualTo('Starting validator Kubeval for ARGO in PLAIN resources')
        assertThat(scriptMock.actualEchoArgs[2]).isEqualTo('Skipping validator HelmKubeval because it is configured as enabled=false or doesn\'t support the given gitopsTool=argo or sourceType=plain')

        assertThat(scriptMock.actualShArgs[0]).isEqualTo('yamllint -f standard staging/app')
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('kubeval -d staging/app -v null --strict --ignore-missing-schemas')
    }
}
