package com.cloudogu.gitopsbuildlib.validation

import com.cloudogu.gitopsbuildlib.deployment.GitopsTool
import com.cloudogu.gitopsbuildlib.deployment.SourceType
import com.cloudogu.gitopsbuildlib.docker.DockerWrapper
import org.codehaus.groovy.runtime.NullObject

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
            withDockerImage(getImage(gitopsConfig, validatorConfig)) {
                validate(targetDirectory, validatorConfig, gitopsConfig)
            }
        } else {
            script.echo "Skipping validator ${this.getClass().getSimpleName()} because it is configured as enabled=false or doesn't support the given gitopsTool=${gitopsTool.name()} or sourceType=${sourceType.name()}"
        }
    }

    abstract protected void validate(String targetDirectory, Map validatorConfig, Map gitopsConfig)
    abstract SourceType[] getSupportedSourceTypes()
    abstract GitopsTool[] getSupportedGitopsTools()

    protected void withDockerImage(String image, Closure body) {
        dockerWrapper.withDockerImage(image, body)
    }

    protected String getImage(Map gitopsConfig, Map validatorConfig) {
        if (validatorConfig.containsKey('image')) {
            return validatorConfig.image
        } else if (validatorConfig.containsKey('imageRef') && gitopsConfig.buildImages.containsKey(validatorConfig.imageRef)) {
            return gitopsConfig.buildImages[validatorConfig.imageRef]
        } else {
            return null
        }
    }
}
