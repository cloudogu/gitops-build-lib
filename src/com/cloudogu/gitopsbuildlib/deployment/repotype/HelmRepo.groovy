package com.cloudogu.gitopsbuildlib.deployment.repotype

class HelmRepo extends RepoType{

    HelmRepo(def script, Map gitopsConfig) {
        super(script, gitopsConfig)
    }

    @Override
    def createRelease(String stage) {
        def helmConfig = gitopsConfig.deployments.helm
        def application = gitopsConfig.application
        def sourcePath = gitopsConfig.deployments.sourcePath

        script.writeFile file: "${stage}/${application}/mergedValues.yaml", text: mergeValues(helmConfig, ["${script.env.WORKSPACE}/${sourcePath}/values-${stage}.yaml", "${script.env.WORKSPACE}/${sourcePath}/values-shared.yaml"] as String[])

        updateYamlValue("${stage}/${application}/mergedValues.yaml", helmConfig)

        script.writeFile file: "${stage}/${application}/helmRelease.yaml", text: createHelmRelease(helmConfig, application, "fluxv1-${stage}", "${stage}/${application}/mergedValues.yaml")

        script.sh "rm ${stage}/${application}/mergedValues.yaml"

        createFileConfigmaps(stage, application, sourcePath, gitopsConfig)
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

    private String mergeValues(Map helmConfig, String[] files) {
        String merge = ""
        String _files = ""
        files.each {
            _files += "-f $it "
        }

        withHelm {
            script.sh "helm repo add chartRepo ${helmConfig.repoUrl}"
            script.sh "helm repo update"
            script.sh "helm pull chartRepo/${helmConfig.chartName} --version=${helmConfig.version} --untar --untardir=${script.env.WORKSPACE}/chart"
            String helmScript = "helm values ${script.env.WORKSPACE}/chart/${helmConfig.chartName} ${_files}"
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
    repository: ${helmConfig.repoUrl}
    name: ${helmConfig.chartName}
    version: ${helmConfig.version}
  values:
    ${values}
"""
    }

    private String fileToInlineYaml(String fileContents) {
        String values = ""
        String indent = "    "
        String fileContent = script.readFile fileContents
        boolean first = true
        fileContent.split("\n").each { line ->
            if(line.size() > 0) {
                if(first) {
                    values += line + "\n"
                    first = false
                } else {
                    values += indent + line + "\n"
                }
            } else {
                values += line + "\n"
            }
        }
        return values
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

    private void withKubectl(Closure body) {
        script.cesBuildLib.Docker.new(script).image(kubectlImage)
        // Allow accessing WORKSPACE even when we are in a child dir (using "dir() {}")
            .inside("${script.pwd().equals(script.env.WORKSPACE) ? '' : "-v ${script.env.WORKSPACE}:${script.env.WORKSPACE}"}") {
                body()
            }
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