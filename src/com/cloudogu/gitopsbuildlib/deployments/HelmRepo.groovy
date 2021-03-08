package com.cloudogu.gitopsbuildlib.deployments

class HelmRepo extends RepoType{

    HelmRepo(Map gitopsConfig) {
        super(gitopsConfig)
    }

    @Override
    protected generateFoldersAndFiles(String stage) {
        def helmConfig = gitopsConfig.deployments.helm
        def application = gitopsConfig.application
        def sourcePath = gitopsConfig.deployments.sourcePath




        script.sh "mkdir -p ${stage}/${application}/"

        //TODO extraresources kopieren?
        script.echo "Copying extra resources from application repo to gitOps Repo: '${sourcePath}/${stage}/*' to '${stage}/${application}'"
        script.sh "cp -a ${script.env.WORKSPACE}/${sourcePath}/${stage}/. ${stage}/${application}/ || true"






        script.writeFile file: "${stage}/${application}/helmRelease.yaml", text: createHelmRelease(helmConfig, application, "fluxv1-${stage}", createFromFileValues(stage, gitopsConfig))
        script.writeFile file: "${stage}/${application}/valuesMap.yaml", text: createConfigMap("values.yaml", "${script.env.WORKSPACE}/${sourcePath}/values-${stage}.yaml", "${application}-helm-operator-values", "fluxv1-${stage}")

        script.writeFile file: "${stage}/${application}/sharedValuesMap.yaml", text: createConfigMap("values.yaml", "${script.env.WORKSPACE}/${sourcePath}/values-shared.yaml", "${application}-shared-helm-operator-values", "fluxv1-${stage}")

        creatFileConfigmaps(stage, application, sourcePath, gitopsConfig)
    }

    private String createHelmRelease(Map helmConfig, String application, String namespace, String extraValues) {
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

    private String createFromFileValues(String stage, Map gitopsConfig) {
        String values = ""

        gitopsConfig.helmValuesFromFile.each {
            if (stage in it['stage']) {
                values = fileToInlineYaml(it['key'], "${script.env.WORKSPACE}/k8s/${it['file']}")
            }
        }
        return values
    }

    private void creatFileConfigmaps(String stage, String application, String sourcePath, Map gitopsConfig) {
        gitopsConfig.fileConfigmaps.each {
            if(stage in it['stage']) {
                String key = it['file'].split('/').last()
                script.writeFile file: "${stage}/${application}/${it['name']}.yaml", text: createConfigMap(key, "${script.env.WORKSPACE}/${sourcePath}/${it['file']}", it['name'], "fluxv1-${stage}")
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

    private String fileToInlineYaml(String key, String filePath) {
        String values = ""
        String indent = "        "

        def fileContent = readFile filePath
        values += "\n    ${key}: |\n${indent}"
        values += fileContent.split("\\n").join("\n" + indent)

        return values
    }
}
