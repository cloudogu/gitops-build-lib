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
    void validate(String targetDirectory, Map config, Map deployments) {
        if (deployments.containsKey('helm')) {

            String args = argsParser.parse(config)

            def chartDir = ''
            if (deployments.helm.containsKey('chartPath')) {
                chartDir = deployments.helm.chartPath
            } else if ( deployments.helm.containsKey('chartName')) {
                chartDir = deployments.helm.chartName
            }

            withDockerImage(config.image) {
                script.sh "helm kubeval chart/${chartDir} -v ${config.k8sSchemaVersion}${args}"
            }
        }
    }
}
