package com.cloudogu.gitopsbuildlib.validation

import com.cloudogu.gitopsbuildlib.deployment.GitopsTool
import com.cloudogu.gitopsbuildlib.deployment.SourceType
import com.cloudogu.gitopsbuildlib.validation.utils.KubevalArgsParser

/**
 * Validates all yaml-resources within the target-directory against the specs of the given k8s version
 */
class Kubeval extends Validator {

    KubevalArgsParser argsParser

    Kubeval(def script) {
        super(script)
        argsParser = new KubevalArgsParser()
    }

    @Override
    void validate(String targetDirectory, Map validatorConfig, Map gitopsConfig) {
        String args = argsParser.parse(validatorConfig)
        script.sh "kubeval -d ${targetDirectory} -v ${validatorConfig.k8sSchemaVersion}${args}"
    }

    @Override
    SourceType[] getSupportedSourceTypes() {
        return [SourceType.PLAIN]
    }

    @Override
    GitopsTool[] getSupportedGitopsTools() {
        return [GitopsTool.ARGO, GitopsTool.FLUX]
    }
}
