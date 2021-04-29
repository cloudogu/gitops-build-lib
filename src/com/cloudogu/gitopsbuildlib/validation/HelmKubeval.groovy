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
            if (deployments.helm.containsKey('chartPath')) {
                chartDir = deployments.helm.chartPath
            } else if ( deployments.helm.containsKey('chartName')) {
                chartDir = deployments.helm.chartName
            }

            withDockerImage(validatorConfig.image) {
                script.sh "helm kubeval ${targetDirectory}/chart/${chartDir} -f ${targetDirectory}/mergedValues.yaml -v ${validatorConfig.k8sSchemaVersion}${args}"
            }
        } else {
            script.echo "Not executing HelmKubeval because this is not a helm deployment"
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
