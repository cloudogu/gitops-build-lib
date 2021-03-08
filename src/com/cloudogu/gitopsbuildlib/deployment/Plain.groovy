package com.cloudogu.gitopsbuildlib.deployment

class Plain extends Deployment{

    Plain(def script, def gitopsConfig) {
        super(script, gitopsConfig)
    }

    @Override
    preparePostValidation(String stage) {
        updateImage(stage)
    }

    def updateImage(String stage) {
        gitopsConfig.deployments.plain.updateImages.each {
            def deploymentFilePath = "${stage}/${gitopsConfig.application}/${gitopsConfig.deployments.sourcePath}/${it['filename']}"
            def data = script.readYaml file: deploymentFilePath
            def containers = data.spec.template.spec.containers
            def containerName = it['containerName']
            def updateContainer = containers.find { it.name == containerName }
            updateContainer.image = it['imageName']
            script.writeYaml file: deploymentFilePath, data: data, overwrite: true
        }
    }

    @Override
    def preparePreValidation(String stage) {
    }
}
