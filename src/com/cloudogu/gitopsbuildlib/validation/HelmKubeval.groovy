package com.cloudogu.gitopsbuildlib.validation

import com.cloudogu.gitopsbuildlib.validation.utils.ArgsParser
import com.cloudogu.gitopsbuildlib.validation.utils.KubevalArgsParser


class HelmKubeval extends Validator {

    ArgsParser argsParser

    HelmKubeval(def script) {
        super(script)
        argsParser = new KubevalArgsParser()
    }

    @Override
    void validate(String targetDirectory, Map validatorConfig, Map gitopsConfig) {
        Map deployments = gitopsConfig.deployments as Map
        if (deployments.containsKey('helm')) {

            String args = argsParser.parse(validatorConfig)

            def chartDir = ''
            if (gitopsConfig.helm.containsKey('chartPath')) {
                chartDir = gitopsConfig.helm.chartPath
            } else if ( gitopsConfig.helm.containsKey('chartName')) {
                chartDir = gitopsConfig.helm.chartName
            }

            withDockerImage(validatorConfig.image) {
                script.sh "helm kubeval chart/${chartDir} -v ${validatorConfig.k8sSchemaVersion}${args}"
            }
        }
    }

    @Override
    SourceType[] getSupportedSourceTypes() {
        return [SourceType.HELM]
    }

    @Override
    GitopsTool[] getSupportedGitopsTools() {
        return [GitopsTool.FLUX]
    }
}
