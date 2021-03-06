package com.cloudogu.gitopsbuildlib.deployments

class Plain extends Deployment{

    static String getConfigDir() { '.config' }

    Plain(def script, def gitopsConfig) {
        super(script, gitopsConfig)
    }

    @Override
    process(String stage) {
        prepareApplicationFolders()
        updateImage(stage)
        validate(stage)
    }

    def prepareApplicationFolders() {
        def sourcePath = gitopsConfig.deployments.sourcePath
        script.sh "mkdir -p ${stage}/${gitopsConfig.application}/"
        script.sh "mkdir -p ${configDir}/"
        // copy extra resources like sealed secrets
        script.echo "Copying k8s payload from application repo to gitOps Repo: '${sourcePath}/${stage}/*' to '${stage}/${gitopsConfig.application}'"
        script.sh "cp ${script.env.WORKSPACE}/${sourcePath}/${stage}/* ${stage}/${gitopsConfig.application}/ || true"
        script.sh "cp ${script.env.WORKSPACE}/*.yamllint.yaml ${configDir}/ || true"
    }

    def updateImage(String stage) {
        gitopsConfig.deployments.plain.updateImages.each {
            def deploymentFilePath = "${stage}/${gitopsConfig.application}/${it['filename']}"
            def data = script.readYaml file: deploymentFilePath
            def containers = data.spec.template.spec.containers
            def containerName = it['containerName']
            def updateContainer = containers.find { it.name == containerName }
            updateContainer.image = it['imageName']
            script.writeYaml file: deploymentFilePath, data: data, overwrite: true
        }
    }
}
