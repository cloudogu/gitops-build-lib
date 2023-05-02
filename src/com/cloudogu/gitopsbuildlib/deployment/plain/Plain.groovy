package com.cloudogu.gitopsbuildlib.deployment.plain

import com.cloudogu.gitopsbuildlib.deployment.Deployment
import com.cloudogu.gitopsbuildlib.deployment.SourceType

class Plain extends Deployment {

    Plain(def script, def gitopsConfig) {
        super(script, gitopsConfig)
    }

    @Override
    def preValidation(String stage) {
        updateImage(stage)
    }

    @Override
    def postValidation(String stage) {
    }

    @Override
    def validate(String stage) {
        def destinationPath = getDestinationFolder(getFolderStructureStrategy(), stage)

        gitopsConfig.validators.each { validator ->
            validator.value.validator.validate(validator.value.enabled, getGitopsTool(), SourceType.PLAIN, "${destinationPath}", validator.value.config, gitopsConfig)
        }
    }

    private updateImage(String stage) {
        def destinationPath = getDestinationFolder(getFolderStructureStrategy(), stage)

        script.echo "About Updating images in plain deployment: ${gitopsConfig.deployments.plain.updateImages}"
        gitopsConfig.deployments.plain.updateImages.each {
            script.echo "Replacing image '${it['imageName']}' in file: ${it['filename']}"
            def deploymentFilePath = "${destinationPath}/${it['filename']}"
            def data = script.readYaml file: deploymentFilePath
            String kind = data.kind
            def containers = findContainers(data, kind) 
            script.echo "Found containers '${containers}' in YAML: ${it['filename']}"
            def containerName = it['containerName']
            def updateContainer = containers.find { it.name == containerName }
            updateContainer.image = it['imageName']
            script.writeYaml file: deploymentFilePath, data: data, overwrite: true
            script.echo "Wrote file ${deploymentFilePath} with yaml:\n${data}"
        }
    }

    def findContainers(def data, String kind) {
        //noinspection GroovyFallthrough
        switch (kind) {
            case 'Deployment':
                // Falling through because Deployment and StatefulSet's paths are the same
            case 'StatefulSet':
                return data.spec.template.spec.containers
            case 'CronJob':
                return data.spec.jobTemplate.spec.template.spec.containers
            default:
                script.echo  "Warning: Kind '$kind' is unknown, using best effort to find 'containers' in YAML"
                // Best effort: Try the same as for Deployment and StatefulSet
                return data.spec.template.spec.containers
        }
    }
}
