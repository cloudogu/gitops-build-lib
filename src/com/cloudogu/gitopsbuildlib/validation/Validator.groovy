package com.cloudogu.gitopsbuildlib.validation

import com.cloudogu.gitopsbuildlib.deployment.GitopsTool
import com.cloudogu.gitopsbuildlib.deployment.SourceType
import com.cloudogu.gitopsbuildlib.docker.DockerWrapper

abstract class Validator {

    protected script
    protected DockerWrapper dockerWrapper

    Validator(def script) {
        this.script = script
        dockerWrapper = new DockerWrapper(script)
    }

    void validate(boolean enabled, GitopsTool gitopsTool, SourceType sourceType, String targetDirectory, Map validatorConfig, Map gitopsConfig) {
        if (enabled && getSupportedGitopsTools().contains(gitopsTool) && getSupportedSourceTypes().contains(sourceType)) {
            script.echo "Starting validator ${this.getClass().getSimpleName()} for ${gitopsTool.name()} in ${sourceType.name()} resources"
            validate(targetDirectory, validatorConfig, gitopsConfig)
        } else {
            script.echo "Skipping validator ${this.getClass().getSimpleName()} because it is configured as enabled=false or doesn't support the given gitopsTool or sourceType"
        }
    }

    abstract protected void validate(String targetDirectory, Map validatorConfig, Map gitopsConfig)
    abstract SourceType[] getSupportedSourceTypes()
    abstract GitopsTool[] getSupportedGitopsTools()

    protected void withDockerImage(String image, Closure body) {
        dockerWrapper.withDockerImage(image, body)
    }
}
