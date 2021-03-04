package com.cloudogu.gitopsbuildlib.deployments

class Helm implements Deployment {

    static String getHelmImage() { 'ghcr.io/cloudogu/helm:3.4.1-1' }
    private def script

    Helm(def script) {
        this.script = script
    }

    @Override
    def prepareApplicationFolders(String stage, Map gitopsConfig) {
        def helmConfig = gitopsConfig.deployments.helm
        def application = gitopsConfig.application
        def sourcePath = gitopsConfig.deployments.sourcePath

        script.sh "mkdir -p ${stage}/${application}/"

        helmConfig.extraResources.each {
            script.sh "cp ${script.env.WORKSPACE}/k8s/${stage}/${it} ${stage}/${application}/ || true"
        }

        if(helmConfig.repoType == 'GIT') {
            // writing the merged-values.yaml via writeYaml into a file has the advantage, that it gets formatted as valid yaml
            // This makes it easier to read in and indent for the inline use in the helmRelease.
            // It enables us to reuse the `fileToInlineYaml` function, without writing a complex formatting logic.
            script.writeFile file: "${stage}/${application}/mergedValues.yaml", text: mergeValues(helmConfig.repoUrl, ["${script.env.WORKSPACE}/${sourcePath}/values-${stage}.yaml", "${script.env.WORKSPACE}/${sourcePath}/values-shared.yaml"] as String[])
        } else if (helmConfig.repoType == 'HELM') {
            script.echo "Copying extra resources from application repo to gitOps Repo: 'k8s/${stage}/*' to '${stage}/${application}'"
            script.sh "cp ${script.env.WORKSPACE}/k8s/${stage}/*.yaml ${stage}/${application}/ || true"

            script.writeFile file: "${stage}/${application}/helmRelease.yaml", text: createHelmRelease(helmChart, "fluxv1-${stage}", createFromFileValues(stage))
            script.writeFile file: "${stage}/${application}/valuesMap.yaml", text: createConfigMap("values.yaml", "${script.env.WORKSPACE}/k8s/values-${stage}.yaml", "${application}-helm-operator-values", "fluxv1-${stage}")

            script.writeFile file: "${stage}/${application}/sharedValuesMap.yaml", text: createConfigMap("values.yaml", "${script.env.WORKSPACE}/k8s/values-shared.yaml", "${application}-shared-helm-operator-values", "fluxv1-${stage}")

            creatFileConfigmaps(stage)
        }
    }

    @Override
    def update(String stage, Map gitopsConfig) {
        def helmConfig = gitopsConfig.deployments.helm
        def application = gitopsConfig.application

        if (gitopsConfig.deployments.helm.repoType == 'GIT') {
            updateYamlValue("${stage}/${application}/mergedValues.yaml", helmConfig)
            script.writeFile file: "${stage}/${application}/helmRelease.yaml", text: createHelmRelease(helmConfig, application, "fluxv1-${stage}", "${stage}/${application}/mergedValues.yaml")
            // since the values are already inline (helmRelease.yaml) we do not need to commit them into the gitops repo
            script.sh "rm ${stage}/${application}/mergedValues.yaml"
        } else if (gitopsConfig.deployments.helm.repoType == 'HELM') {

        }
    }
    /////////////////////////////////////////////////////// git type //////////////////////////////////////////////////////

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

    private String createHelmRelease(Map helmConfig, String application, String namespace, String valuesFile) {
        def values = fileToInlineYaml(valuesFile)
        return """apiVersion: helm.fluxcd.io/v1
kind: HelmRelease
metadata:
  name: ${application}
  namespace: ${namespace}
  annotations:
    fluxcd.io/automated: "false"
spec:
  releaseName: ${application}
  chart:
    git: ${helmConfig.repoUrl}
    ref: ${helmConfig.version}
    path: .
  values:
    ${values}
"""
    }

    private String fileToInlineYaml(String fileContents) {
        String values = ""
        String indent = "    "
        def fileContent = script.readFile fileContents
        values += fileContent.split("\\n").join("\n" + indent)
        return values
    }

    private String mergeValues(String chart, String[] files) {
        String merge = ""
        String _files = ""
        files.each {
            _files += "-f $it "
        }

        script.sh "git clone ${chart} ${script.env.WORKSPACE}/chart || true"

        withHelm {
            String helmScript = "helm values ${script.env.WORKSPACE}/chart ${_files}"
            merge = script.sh returnStdout: true, script: helmScript
        }

        script.sh "rm -rf ${script.env.WORKSPACE}/chart || true"

        return merge
    }

    private void withHelm(Closure body) {
        script.cesBuildLib.Docker.new(script).image(helmImage).inside(
            "${script.pwd().equals(script.env.WORKSPACE) ? '' : "-v ${script.env.WORKSPACE}:${script.env.WORKSPACE}"}"
        ) {
            body()
        }
    }

/////////////////////////////////////////////////////// helm type //////////////////////////////////////////////////////

    private String createHelmRelease(Map helmChart, String namespace, String extraValues) {
        return """apiVersion: helm.fluxcd.io/v1
kind: HelmRelease
metadata:
  name: ${application}
  namespace: ${namespace}
  annotations:
    fluxcd.io/automated: "false"
spec:
  releaseName: ${application}
  chart:
    repository: ${helmChart.repoUrl}
    name: ${helmChart.chartName}
    version: ${helmChart.version}
    
  valuesFrom:
  - configMapKeyRef:
      name: ${application}-shared-helm-operator-values
      namespace: ${namespace}
      key: values.yaml
      optional: false
  - configMapKeyRef:
      name: ${application}-helm-operator-values
      namespace: ${namespace}
      key: values.yaml
      optional: false
"""
    }

    private String createFromFileValues(String stage) {
        String values = ""

        helmValuesFromFile.each {
            if (stage in it['stage']) {
                values = fileToInlineYaml(it['key'], "${script.env.WORKSPACE}/k8s/${it['file']}")
            }
        }
        return values
    }

    private void creatFileConfigmaps(String stage) {
        fileConfigmaps.each {
            if(stage in it['stage']) {
                String key = it['file'].split('/').last()
                script.writeFile file: "${stage}/${application}/${it['name']}.yaml", text: createConfigMap(key, "${env.WORKSPACE}/k8s/${it['file']}", it['name'], "fluxv1-${stage}")
            }
        }
    }

    private String createConfigMap(String key, String filePath, String name, String namespace) {
        String configMap = ""
        withKubectl {
            String script = "KUBECONFIG=${writeKubeConfig()} kubectl create configmap ${name} " +
                "--from-file=${key}=${filePath} " +
                "--dry-run=client -o yaml -n ${namespace}"

            configMap = sh returnStdout: true, script: script
        }
        return configMap
    }

// Dummy kubeConfig, so we can use `kubectl --dry-run=client`
    private String writeKubeConfig() {
        String kubeConfigPath = "${pwd()}/.kube/config"
        echo "Writing $kubeConfigPath"
        writeFile file: kubeConfigPath, text: """apiVersion: v1
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

    private void withKubectl(Closure body) {
        script.cesBuildLib.Docker.new(script).image(kubectlImage)
        // Allow accessing WORKSPACE even when we are in a child dir (using "dir() {}")
            .inside("${pwd().equals(env.WORKSPACE) ? '' : "-v ${env.WORKSPACE}:${env.WORKSPACE}"}") {
                body()
            }
    }

    private String fileToInlineYaml(String key, String filePath) {
        String values = ""
        String indent = "        "

        def fileContent = readFile filePath
        values += "\n    ${key}: |\n${indent}"
        values += fileContent.split("\\n").join("\n" + indent)

        return values
    }

}
