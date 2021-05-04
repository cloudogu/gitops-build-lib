package com.cloudogu.gitopsbuildlib.deployment.helm

import com.cloudogu.gitopsbuildlib.deployment.Deployment
import com.cloudogu.gitopsbuildlib.deployment.GitopsTool
import com.cloudogu.gitopsbuildlib.deployment.helm.helmrelease.ArgoCDRelease
import com.cloudogu.gitopsbuildlib.deployment.helm.helmrelease.FluxV1Release
import com.cloudogu.gitopsbuildlib.deployment.helm.helmrelease.HelmRelease
import com.cloudogu.gitopsbuildlib.deployment.helm.repotype.GitRepo
import com.cloudogu.gitopsbuildlib.deployment.helm.repotype.HelmRepo
import com.cloudogu.gitopsbuildlib.deployment.helm.repotype.RepoType
import com.cloudogu.gitopsbuildlib.deployment.SourceType

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
        }
        if(getGitopsTool() == GitopsTool.FLUX) {
            helmRelease = new FluxV1Release(script)
        } else if(getGitopsTool() == GitopsTool.ARGO) {
            helmRelease = new ArgoCDRelease(script)
        }
    }

    @Override
    def preValidation(String stage) {
        def helmConfig = gitopsConfig.deployments.helm
        def application = gitopsConfig.application
        def sourcePath = gitopsConfig.deployments.sourcePath

        chartRepo.prepareRepo(helmConfig, helmChartTempDir, chartRootDir)

        // writing the merged-values.yaml via writeYaml into a file has the advantage, that it gets formatted as valid yaml
        // This makes it easier to read in and indent for the inline use in the helmRelease.
        // It enables us to reuse the `fileToInlineYaml` function, without writing a complex formatting logic.
        script.writeFile file: "${script.env.WORKSPACE}/${helmChartTempDir}/mergedValues.yaml", text: mergeValuesFiles(helmConfig, ["${script.env.WORKSPACE}/${sourcePath}/values-${stage}.yaml", "${script.env.WORKSPACE}/${sourcePath}/values-shared.yaml"] as String[])

        updateYamlValue("${script.env.WORKSPACE}/${helmChartTempDir}/mergedValues.yaml", helmConfig)

        script.writeFile file: "${stage}/${application}/applicationRelease.yaml", text: helmRelease.create(helmConfig, application, getNamespace(stage), "${script.env.WORKSPACE}/${helmChartTempDir}/mergedValues.yaml")
    }

    @Override
    def postValidation(String stage) {
        // clean the helm chart folder since the validation on this helm chart is done
        script.sh "rm -rf ${script.env.WORKSPACE}/${helmChartTempDir} || true"
    }

    @Override
    def validate(String stage) {
        gitopsConfig.validators.each { validator ->
            validator.value.validator.validate(validator.value.enabled, getGitopsTool(), SourceType.PLAIN, "${stage}/${gitopsConfig.application}", validator.value.config, gitopsConfig)
            validator.value.validator.validate(validator.value.enabled, getGitopsTool(), SourceType.HELM, "${script.env.WORKSPACE}/${helmChartTempDir}",validator.value.config, gitopsConfig)
        }
    }

    private void updateYamlValue(String yamlFilePath, Map helmConfig) {
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

    private String mergeValuesFiles(Map helmConfig, String[] valuesFiles) {
        String mergedValuesFile = ""

        def chartDir = ''
        if (helmConfig.containsKey('chartPath') && helmConfig.chartPath) {
            chartDir = helmConfig.chartPath
        } else if ( helmConfig.containsKey('chartName')) {
            chartDir = helmConfig.chartName
        }

        withHelm {
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
