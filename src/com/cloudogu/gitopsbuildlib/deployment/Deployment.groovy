package com.cloudogu.gitopsbuildlib.deployment

abstract class Deployment {

    static String getConfigDir() { '.config' }

    protected script
    protected stage
    protected gitopsConfig

    Deployment(def script, def gitopsConfig) {
        this.script = script
        this.gitopsConfig = gitopsConfig
    }

    def deploy(String stage) {
        createFoldersAndCopyK8sResources(stage)
        processPreValidation(stage)
        validate(stage)
        processPostValidation(stage)
    }

    abstract processPreValidation(String stage)

    abstract processPostValidation(String stage)

    def validate(String stage) {
        gitopsConfig.validators.each { validatorConfig ->
            script.echo "Executing validator ${validatorConfig.key}"
            def targetDirectory = "${stage}/${gitopsConfig.application}"
            if(validatorConfig.key.equals('kubeval')) {
                targetDirectory += "/${gitopsConfig.deployments.sourcePath}"
            }
            validatorConfig.value.validator.validate(validatorConfig.value.enabled, targetDirectory, validatorConfig.value.config, gitopsConfig.deployments)
        }
    }

    def createFoldersAndCopyK8sResources(String stage) {
        def sourcePath = gitopsConfig.deployments.sourcePath
        def application = gitopsConfig.application

        script.sh "mkdir -p ${stage}/${application}/"
        script.sh "mkdir -p ${configDir}/"
        // copy extra resources like sealed secrets
        script.echo "Copying k8s payload from application repo to gitOps Repo: '${sourcePath}/${stage}/*' to '${stage}/${application}/${sourcePath}'"
        script.sh "cp ${script.env.WORKSPACE}/${sourcePath}/${stage}/* ${stage}/${application}/${sourcePath}/ || true"
        script.sh "cp ${script.env.WORKSPACE}/*.yamllint.yaml ${configDir}/ || true"
    }
}
