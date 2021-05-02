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
        String args = argsParser.parse(validatorConfig)

        withDockerImage(validatorConfig.image) {
            script.sh "helm kubeval ${targetDirectory}/chart/${getChartDir(deployments)} -f ${targetDirectory}/mergedValues.yaml -v ${validatorConfig.k8sSchemaVersion}${args}"
        }
    }

    private String getChartDir(Map deployments) {
        def chartDir = ''
        if (deployments.helm.containsKey('chartPath')) {
            chartDir = deployments.helm.chartPath
        } else if ( deployments.helm.containsKey('chartName')) {
            chartDir = deployments.helm.chartName
        }
        return chartDir
    }

    @Override
    SourceType[] getSupportedSourceTypes() {
        return [SourceType.HELM]
    }

    @Override
    GitopsTool[] getSupportedGitopsTools() {
        return [GitopsTool.FLUX]
    }

    @Override
    Deployment[] getSupportedDeployments() {
        return [Deployment.HELM]
    }
}
