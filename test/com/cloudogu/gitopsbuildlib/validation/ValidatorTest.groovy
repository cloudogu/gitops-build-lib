package com.cloudogu.gitopsbuildlib.validation

import com.cloudogu.gitopsbuildlib.ScriptMock
import com.cloudogu.gitopsbuildlib.docker.DockerWrapper
import com.cloudogu.gitopsbuildlib.validation.Validator
import org.junit.jupiter.api.Test
import static org.assertj.core.api.Assertions.assertThat

class ValidatorTest {
    def scriptMock = new ScriptMock()
    def dockerMock = scriptMock.dockerMock
    def validator = new ValidatorUnderTest(scriptMock.mock)
    boolean closureCalled = false
    boolean validateCalled = false

    @Test
    void 'withDockerImage mounts workspace'() {
        validator.validate(true, 'target', [:], [:])
        assertThat(dockerMock.actualInsideArgs[0]).isEqualTo('-v workspace:workspace --entrypoint=""')
        assertThat(closureCalled).as("Closure was not called").isTrue()
        assertThat(validateCalled).as("Validate was not called").isTrue()
    }

    @Test
    void 'withDockerImage doesnt mount workspace if already in workspace'() {
        scriptMock.mock.pwd = { scriptMock.mock.env.WORKSPACE }
        validator.validate(true, 'target', [:], [:])
        assertThat(dockerMock.actualInsideArgs[0]).isEqualTo('--entrypoint=""')
        assertThat(closureCalled).as("Closure was not called").isTrue()
        assertThat(validateCalled).as("Validate was not called").isTrue()
    }

    @Test
    void 'skip validator if disabled'() {
        validator.validate(false, 'target', [:], [:])
        assertThat(validateCalled).as("Validate was called").isFalse()
        assertThat(scriptMock.actualEchoArgs[0])
            .isEqualTo("Skipping validator ValidatorUnderTest because it is configured as enabled=false")
    }

    @Test
    void 'correct target directory for helm sourceType'() {
        def output = validator.getTargetDirectory("staging", "app", SourceType.HELM)

        assertThat(output).isEqualTo("workspace/.helmChartTempDir")
    }

    @Test
    void 'correct target directory for plain sourceType'() {
        def output = validator.getTargetDirectory("staging", "app", SourceType.PLAIN)

        assertThat(output).isEqualTo("staging/app")
    }

    class ValidatorUnderTest extends Validator {

        ValidatorUnderTest(Object script) {
            super(script)
        }

        @Override
        void validate(String targetDirectory, Map config, Map gitopsConfig) {
            validateCalled = true
            withDockerImage('') {
                closureCalled = true
            }
        }

        @Override
        SourceType[] getSupportedSourceTypes() {
            return [SourceType.PLAIN]
        }

        @Override
        GitopsTool[] getSupportedGitopsTools() {
            return [GitopsTool.FLUX]
        }
    }
}
