package com.cloudogu.gitopsbuildlib.deployment

abstract class Deployment {

    static String getConfigDir() { '.config' }

    protected script
    protected gitopsConfig

    Deployment(def script, def gitopsConfig) {
        this.script = script
        this.gitopsConfig = gitopsConfig
    }

    def create(String stage) {
        createFoldersAndCopyK8sResources(stage)
        createPreValidation(stage)
        validate(stage)
        createPostValidation(stage)
    }

    abstract createPreValidation(String stage)

    abstract createPostValidation(String stage)

    def validate(String stage) {
        gitopsConfig.validators.each { validatorConfig ->
            script.echo "Executing validator ${validatorConfig.key}"
            validatorConfig.value.validator.validate(validatorConfig.value.enabled, "${stage}/${gitopsConfig.application}", validatorConfig.value.config, gitopsConfig.deployments)
        }
    }

    def createFoldersAndCopyK8sResources(String stage) {
        def sourcePath = gitopsConfig.deployments.sourcePath
        def application = gitopsConfig.application

        script.sh "mkdir -p ${stage}/${application}/${sourcePath}/"
        script.sh "mkdir -p ${configDir}/"
        // copy extra resources like sealed secrets
        script.echo "Copying k8s payload from application repo to gitOps Repo: '${sourcePath}/${stage}/*' to '${stage}/${application}/${sourcePath}'"
        script.sh "cp -r ${script.env.WORKSPACE}/${sourcePath}/${stage}/* ${stage}/${application}/${sourcePath}/ || true"
        script.sh "cp ${script.env.WORKSPACE}/*.yamllint.yaml ${configDir}/ || true"
    }
}
