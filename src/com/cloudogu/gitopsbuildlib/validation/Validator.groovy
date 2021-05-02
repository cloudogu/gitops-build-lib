package com.cloudogu.gitopsbuildlib.validation

import com.cloudogu.gitopsbuildlib.docker.DockerWrapper

abstract class Validator {

    protected script
    protected DockerWrapper dockerWrapper

    Validator(def script) {
        this.script = script
        dockerWrapper = new DockerWrapper(script)
    }

    void validate(boolean enabled, String stage, Map validatorConfig, Map gitopsConfig) {
        GitopsTool gitopsTool = gitopsConfig.gitopsTool.toUpperCase()
        Deployment deployment = getDeploymentType(gitopsConfig)
        if (enabled && getSupportedGitopsTools().contains(gitopsTool) && getSupportedDeployments().contains(deployment)) {
            getSupportedSourceTypes().each { sourceType ->
                script.echo "Starting validator ${this.getClass().getSimpleName()} for ${gitopsTool.name()} in ${sourceType.name()} resources"
                validate(getTargetDirectory(stage, gitopsConfig.application, sourceType), validatorConfig, gitopsConfig)
            }
        } else {
            script.echo "Skipping validator ${this.getClass().getSimpleName()} because it is configured as enabled=false or doesn't support the given gitopsTool or deployment"
        }
    }

    abstract protected void validate(String targetDirectory, Map validatorConfig, Map gitopsConfig)
    abstract SourceType[] getSupportedSourceTypes()
    abstract GitopsTool[] getSupportedGitopsTools()
    abstract Deployment[] getSupportedDeployments()

    protected Deployment getDeploymentType(Map gitopsConfig) {
        Deployment deploymentType = null
        if (gitopsConfig.deployments.containsKey('helm')) {
            deploymentType = Deployment.HELM
        } else if (gitopsConfig.deployments.containsKey('plain')) {
            deploymentType = Deployment.PLAIN
        }
        return deploymentType
    }

    protected String getTargetDirectory(String stage, String application, SourceType sourceType) {
        switch (sourceType) {
            case SourceType.HELM:
                return "${script.env.WORKSPACE}/.helmChartTempDir"
            case SourceType.PLAIN:
                return  "${stage}/${application}"
        }
    }

    protected void withDockerImage(String image, Closure body) {
        dockerWrapper.withDockerImage(image, body)
    }
}
