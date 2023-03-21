package com.cloudogu.gitopsbuildlib.deployment.helm

import com.cloudogu.gitopsbuildlib.deployment.Deployment
import com.cloudogu.gitopsbuildlib.deployment.SourceType
import com.cloudogu.gitopsbuildlib.deployment.helm.helmrelease.ArgoCDRelease
import com.cloudogu.gitopsbuildlib.deployment.helm.helmrelease.FluxV1Release
import com.cloudogu.gitopsbuildlib.deployment.helm.helmrelease.HelmRelease
import com.cloudogu.gitopsbuildlib.deployment.helm.repotype.GitRepo
import com.cloudogu.gitopsbuildlib.deployment.helm.repotype.HelmRepo
import com.cloudogu.gitopsbuildlib.deployment.helm.repotype.LocalRepo
import com.cloudogu.gitopsbuildlib.deployment.helm.repotype.RepoType 

class Helm extends Deployment {

    protected RepoType chartRepo
    protected HelmRelease helmRelease

    private String helmChartTempDir = ".helmChartTempDir"
    private String chartRootDir = "chart"

    Helm(def script, def gitopsConfig) {
        super(script, gitopsConfig)
        this.extraResourcesFolder = "extraResources"
        
        if (gitopsConfig.deployments.helm.repoType == 'GIT') {
            chartRepo = new GitRepo(script)
        } else if (gitopsConfig.deployments.helm.repoType == 'HELM') {
            chartRepo = new HelmRepo(script)
        } else if (gitopsConfig.deployments.helm.repoType == 'LOCAL') {
            chartRepo = new LocalRepo(script)
        } else {
            script.error("Unknown helm repo type: ${gitopsConfig.deployments.helm.repoType}")
        }

        if(gitopsConfig.gitopsTool == 'FLUX') {
            helmRelease = new FluxV1Release(script)
        } else if(gitopsConfig.gitopsTool == 'ARGO') {
            helmRelease = new ArgoCDRelease(script)
        }
    }


    @Override
    def preValidation(String stage) {
        def sourcePath = gitopsConfig.deployments.sourcePath
        def destinationPath = getDestinationFolder(getFolderStructureStrategy(), stage)

        chartRepo.prepareRepo(gitopsConfig, helmChartTempDir, chartRootDir)

        // writing the merged-values.yaml via writeYaml into a file has the advantage, that it gets formatted as valid yaml
        // This makes it easier to read in and indent for the inline use in the helmRelease.
        // It enables us to reuse the `fileToInlineYaml` function, without writing a complex formatting logic.

        def valueFiles = ["${script.env.WORKSPACE}/${sourcePath}/values-${stage}.yaml"]
        // only add values-shared.yaml, if it exists
        if (script.fileExists("${script.env.WORKSPACE}/${sourcePath}/values-shared.yaml")) {
            valueFiles.add("${script.env.WORKSPACE}/${sourcePath}/values-shared.yaml")
        }
        script.writeFile file: "${script.env.WORKSPACE}/${helmChartTempDir}/mergedValues.yaml", text: mergeValuesFiles(gitopsConfig, valueFiles as String[])

        updateYamlValue("${script.env.WORKSPACE}/${helmChartTempDir}/mergedValues.yaml", gitopsConfig)

        script.writeFile file: "${destinationPath}/applicationRelease.yaml", text: helmRelease.create(gitopsConfig, getNamespace(stage), "${script.env.WORKSPACE}/${helmChartTempDir}/mergedValues.yaml")
    }

    @Override
    def postValidation(String stage) {
        // clean the helm chart folder since the validation on this helm chart is done
        script.sh "rm -rf ${script.env.WORKSPACE}/${helmChartTempDir} || true"
    }

    @Override
    def validate(String stage) {
        def destinationPath = getDestinationFolder(getFolderStructureStrategy(), stage)

        gitopsConfig.validators.each { validator ->
            validator.value.validator.validate(validator.value.enabled, getGitopsTool(), SourceType.PLAIN, "${destinationPath}", validator.value.config, gitopsConfig)
            validator.value.validator.validate(validator.value.enabled, getGitopsTool(), SourceType.HELM, "${script.env.WORKSPACE}/${helmChartTempDir}",validator.value.config, gitopsConfig)
        }
    }

    private void updateYamlValue(String yamlFilePath, Map gitopsConfig) {
        def helmConfig = gitopsConfig.deployments.helm

        def data = script.readYaml file: yamlFilePath
        helmConfig.updateValues.each {
            String[] paths = it["fieldPath"].split("\\.")
            def _tmp = data
            paths.eachWithIndex { String p, int i ->
                def tmp = _tmp.get(p)
                if (i == paths.length - 1 && tmp != null) {
                    _tmp.put(p, it["newValue"])
                }
                _tmp = tmp
            }
        }
        script.writeYaml file: yamlFilePath, data: data, overwrite: true
    }

    private String mergeValuesFiles(Map gitopsConfig, String[] valuesFiles) {
        def helmConfig = gitopsConfig.deployments.helm

        String mergedValuesFile = ""

        def chartDir = ''
        if (helmConfig.containsKey('chartPath') && helmConfig.chartPath) {
            chartDir = helmConfig.chartPath
        } else if ( helmConfig.containsKey('chartName')) {
            chartDir = helmConfig.chartName
        }

        withDockerImage(gitopsConfig.buildImages.helm) {
            String helmScript = "helm values ${script.env.WORKSPACE}/${helmChartTempDir}/${chartRootDir}/${chartDir} ${valuesFilesWithParameter(valuesFiles)}"
            mergedValuesFile = script.sh returnStdout: true, script: helmScript
        }
        return mergedValuesFile
    }

    private String valuesFilesWithParameter(String[] valuesFiles) {
        String valuesFilesWithParameter = ""
        valuesFiles.each {
            valuesFilesWithParameter += "-f $it "
        }
        return valuesFilesWithParameter
    }
}
