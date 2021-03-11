package com.cloudogu.gitopsbuildlib.deployment

import com.cloudogu.gitopsbuildlib.deployment.repotype.GitRepo
import com.cloudogu.gitopsbuildlib.deployment.repotype.HelmRepo
import com.cloudogu.gitopsbuildlib.deployment.repotype.RepoType

class Helm extends Deployment {

    protected RepoType helm

    Helm(def script, def gitopsConfig) {
        super(script, gitopsConfig)
        if(gitopsConfig.deployments.helm.repoType == 'GIT') {
            helm = new GitRepo(script)
        } else if (gitopsConfig.deployments.helm.repoType == 'HELM') {
            helm = new HelmRepo(script)
        }
    }

    @Override
    def createPreValidation(String stage) {
        def helmConfig = gitopsConfig.deployments.helm
        def application = gitopsConfig.application
        def sourcePath = gitopsConfig.deployments.sourcePath

        // writing the merged-values.yaml via writeYaml into a file has the advantage, that it gets formatted as valid yaml
        // This makes it easier to read in and indent for the inline use in the helmRelease.
        // It enables us to reuse the `fileToInlineYaml` function, without writing a complex formatting logic.
        script.writeFile file: "${stage}/${application}/mergedValues.yaml", text: helm.mergeValues(helmConfig, ["${script.env.WORKSPACE}/${sourcePath}/values-${stage}.yaml", "${script.env.WORKSPACE}/${sourcePath}/values-shared.yaml"] as String[])

        updateYamlValue("${stage}/${application}/mergedValues.yaml", helmConfig)
        script.writeFile file: "${stage}/${application}/helmRelease.yaml", text: helm.createHelmRelease(helmConfig, application, getNamespace(stage), "${stage}/${application}/mergedValues.yaml")
        // since the values are already inline (helmRelease.yaml) we do not need to commit them into the gitops repo
        script.sh "rm ${stage}/${application}/mergedValues.yaml"
    }

    @Override
    def createPostValidation(String stage) {
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



    //TODO helmValuesFromFile not yet implemented
//    private String createFromFileValues(String stage, Map gitopsConfig) {
//        String values = ""
//
//        gitopsConfig.helmValuesFromFile.each {
//            if (stage in it['stage']) {
//                values = fileToInlineYaml(it['key'], "${script.env.WORKSPACE}/k8s/${it['file']}")
//            }
//        }
//        return values
//    }
//
//    private String fileToInlineYaml(String key, String filePath) {
//        String values = ""
//        String indent = "        "
//
//        def fileContent = readFile filePath
//        values += "\n    ${key}: |\n${indent}"
//        values += fileContent.split("\\n").join("\n" + indent)
//
//        return values
//    }
}
