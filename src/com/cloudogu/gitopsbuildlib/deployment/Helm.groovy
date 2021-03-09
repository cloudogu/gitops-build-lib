package com.cloudogu.gitopsbuildlib.deployment

import com.cloudogu.gitopsbuildlib.deployment.repotype.GitRepo
import com.cloudogu.gitopsbuildlib.deployment.repotype.HelmRepo
import com.cloudogu.gitopsbuildlib.deployment.repotype.RepoType

class Helm extends Deployment {

    protected static String getKubectlImage() { 'lachlanevenson/k8s-kubectl:v1.19.3' }

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
        script.writeFile file: "${stage}/${application}/helmRelease.yaml", text: helm.createHelmRelease(helmConfig, application, "fluxv1-${stage}", "${stage}/${application}/mergedValues.yaml")
        // since the values are already inline (helmRelease.yaml) we do not need to commit them into the gitops repo
        script.sh "rm ${stage}/${application}/mergedValues.yaml"

        createFileConfigmaps(stage, application, sourcePath, gitopsConfig)
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

    private void createFileConfigmaps(String stage, String application, String sourcePath, Map gitopsConfig) {
        gitopsConfig.fileConfigmaps.each {
            if(stage in it['stage']) {
                String key = it['sourceFilePath'].split('/').last()
                script.writeFile file: "${stage}/${application}/${it['name']}.yaml", text: createConfigMap(key, "${script.env.WORKSPACE}/${sourcePath}/${it['sourceFilePath']}", it['name'], "fluxv1-${stage}")
            }
        }
    }

    private String createConfigMap(String key, String filePath, String name, String namespace) {
        String configMap = ""
        withKubectl {
            String kubeScript = "KUBECONFIG=${writeKubeConfig()} kubectl create configmap ${name} " +
                "--from-file=${key}=${filePath} " +
                "--dry-run=client -o yaml -n ${namespace}"

            configMap = script.sh returnStdout: true, script: kubeScript
        }
        return configMap
    }

    private void withKubectl(Closure body) {
        script.cesBuildLib.Docker.new(script).image(kubectlImage)
        // Allow accessing WORKSPACE even when we are in a child dir (using "dir() {}")
            .inside("${script.pwd().equals(script.env.WORKSPACE) ? '' : "-v ${script.env.WORKSPACE}:${script.env.WORKSPACE}"}") {
                body()
            }
    }

    // Dummy kubeConfig, so we can use `kubectl --dry-run=client`
    private String writeKubeConfig() {
        String kubeConfigPath = "${script.pwd()}/.kube/config"
        script.echo "Writing $kubeConfigPath"
        script.writeFile file: kubeConfigPath, text: """apiVersion: v1
clusters:
- cluster:
    certificate-authority-data: DATA+OMITTED
    server: https://localhost
  name: self-hosted-cluster
contexts:
- context:
    cluster: self-hosted-cluster
    user: svcs-acct-dply
  name: svcs-acct-context
current-context: svcs-acct-context
kind: Config
preferences: {}
users:
- name: svcs-acct-dply
  user:
    token: DATA+OMITTED"""

        return kubeConfigPath
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
