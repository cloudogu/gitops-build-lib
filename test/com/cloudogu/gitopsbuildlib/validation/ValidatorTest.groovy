package com.cloudogu.gitopsbuildlib.validation

import com.cloudogu.gitopsbuildlib.ScriptMock
import com.cloudogu.gitopsbuildlib.deployment.GitopsTool
import com.cloudogu.gitopsbuildlib.deployment.SourceType
import org.junit.jupiter.api.Test
import static org.assertj.core.api.Assertions.assertThat

class ValidatorTest {
    def scriptMock = new ScriptMock()
    def dockerMock = scriptMock.dockerMock
    def validator = new ValidatorUnderTest(scriptMock.mock)
    boolean validateCalled = false

    @Test
    void 'withDockerImage mounts workspace'() {
        validator.validate(true, GitopsTool.ARGO, SourceType.HELM, "helmDir",
        [
            imageRef: 'helm'
        ],
        [
            buildImages: [
                helm: [
                    image: 'helmImage'
                ]
            ]
        ])
        assertThat(dockerMock.actualImages[0]).isEqualTo('helmImage')
        assertThat(dockerMock.actualInsideArgs[0]).isEqualTo('-v workspace:workspace --entrypoint=""')
        assertThat(validateCalled).as("Validate was not called").isTrue()
    }

    @Test
    void 'withDockerImage doesnt mount workspace if already in workspace'() {
        scriptMock.mock.pwd = { scriptMock.mock.env.WORKSPACE }
        validator.validate(true, GitopsTool.ARGO, SourceType.HELM, "helmDir", [
            image: 'helmImage'
        ],
            [
                buildImages: [
                    helm: [
                        image: 'helmImageNotBeingUsed'
                    ]
                ]
            ])
        assertThat(dockerMock.actualImages[0]).isEqualTo('helmImage')
        assertThat(dockerMock.actualInsideArgs[0]).isEqualTo('--entrypoint=""')
        assertThat(validateCalled).as("Validate was not called").isTrue()
    }

    @Test
    void 'skip validator if disabled'() {
        validator.validate(false, GitopsTool.ARGO, SourceType.HELM, "helmDir", [
            imageRef: 'helm'
        ],
            [
                buildImages: [
                    helm: 'helmImage'
                ]
            ])
        assertThat(dockerMock.actualImages[0]).isEqualTo(null)
        assertThat(validateCalled).as("Validate was called").isFalse()
        assertThat(scriptMock.actualEchoArgs[0])
            .isEqualTo("Skipping validator ValidatorUnderTest because it is configured as enabled=false or doesn't support the given gitopsTool=ARGO or sourceType=HELM")
    }

    @Test
    void 'get null if no imageRef or image is set in validator'() {
        def output = validator.getImageConfig([:], [:])

        assertThat(output).isEqualTo(null)
    }

    @Test
    void 'get image if specifically set in validator while also having an imageRef'() {
        def output = validator.getImageConfig(
            [
                buildImages: [
                    notUsedImage: 'nope'
                ]
            ],
            [
            image: 'realImage',
            imageRef: 'buildimage.notUsedImage'
        ])
        
        def expected = [image: 'realImage']

        assertThat(output).isEqualTo(expected)
    }

    @Test
    void 'get image if specifically set in validator whisle also having an imageRef'() {
        def output = validator.getImageConfig(
            [
                buildImages: [
                    usedImage: 'yes'
                ]
            ],
            [

                imageRef: 'usedImage'
            ])

        assertThat(output).isEqualTo('yes')
    }

    class ValidatorUnderTest extends Validator {

        ValidatorUnderTest(Object script) {
            super(script)
        }

        @Override
        void validate(String targetDirectory, Map validatorConfig, Map gitopsConfig) {
            println "validatorundertest validate"
            validateCalled = true
        }

        @Override
        SourceType[] getSupportedSourceTypes() {
            return [SourceType.PLAIN, SourceType.HELM]
        }

        @Override
        GitopsTool[] getSupportedGitopsTools() {
            return [GitopsTool.FLUX, GitopsTool.ARGO]
        }
    }
}
