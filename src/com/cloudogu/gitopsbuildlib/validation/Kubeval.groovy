package com.cloudogu.gitopsbuildlib.validation

import com.cloudogu.gitopsbuildlib.validation.utils.ArgsParser
import com.cloudogu.gitopsbuildlib.validation.utils.KubevalArgsParser

/**
 * Validates all yaml-resources within the target-directory against the specs of the given k8s version
 */
class Kubeval extends Validator {

    ArgsParser argsParser

    Kubeval(def script) {
        super(script)
        argsParser = new KubevalArgsParser()
    }

    @Override
    void validate(String targetDirectory, Map config, Map gitopsConfig) {

        String args = argsParser.parse(config)

        withDockerImage(config.image) {
            script.sh "kubeval -d ${targetDirectory} -v ${config.k8sSchemaVersion}${args}"
        }
    }

    @Override
    SourceType[] getSupportedSourceTypes() {
        return [SourceType.PLAIN]
    }

    @Override
    GitopsTool[] getSupportedGitopsTools() {
        return [GitopsTool.ARGO, GitopsTool.FLUX]
    }

    @Override
    Deployment[] getSupportedDeployments() {
        return [Deployment.HELM, Deployment.PLAIN]
    }
}
