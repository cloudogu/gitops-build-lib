package com.cloudogu.gitopsbuildlib.deployment.plain

import com.cloudogu.gitopsbuildlib.deployment.Deployment

class Plain extends Deployment{

    Plain(def script, def gitopsConfig) {
        super(script, gitopsConfig)
    }

    @Override
    def preValidation(String stage) {
    }

    @Override
    def postValidation(String stage) {
        updateImage(stage)
    }

    private updateImage(String stage) {
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
