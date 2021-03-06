package com.cloudogu.gitopsbuildlib.deployments

abstract class Deployment {

    protected script
    protected stage
    protected gitopsConfig

    Deployment(def script, def gitopsConfig) {
        this.script = script
        this.gitopsConfig = gitopsConfig
    }


    abstract process(String stage)

    validate(String stage) {
        gitopsConfig.validators.each { validatorConfig ->
            script.echo "Executing validator ${validatorConfig.key}"

            validatorConfig.value.validator.validate(validatorConfig.value.enabled, "${stage}/${gitopsConfig.application}", validatorConfig.value.config, gitopsConfig.deployments)
        }
    }
}
