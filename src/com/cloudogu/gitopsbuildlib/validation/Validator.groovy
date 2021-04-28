package com.cloudogu.gitopsbuildlib.validation

import com.cloudogu.gitopsbuildlib.docker.DockerWrapper

abstract class Validator {

    protected script
    protected DockerWrapper dockerWrapper

    Validator(def script) {
        this.script = script
        dockerWrapper = new DockerWrapper(script)
    }

    void validate(boolean enabled, String targetDirectory, Map validatorConfig, Map gitopsConfig) {
        if (enabled) {
            validate(targetDirectory, validatorConfig, gitopsConfig)
        } else {
            script.echo "Skipping validator ${this.getClass().getSimpleName()} because it is configured as enabled=false"
        }
    }

    abstract protected void validate(String targetDirectory, Map validatorConfig, Map gitopsConfig)
    abstract SourceType[] getSupportedSourceTypes()
    abstract GitopsTool[] getSupportedGitopsTools()

    protected void withDockerImage(String image, Closure body) {
        dockerWrapper.withDockerImage(image, body)
    }
}
