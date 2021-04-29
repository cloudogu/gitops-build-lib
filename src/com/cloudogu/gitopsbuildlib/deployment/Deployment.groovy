package com.cloudogu.gitopsbuildlib.deployment

import com.cloudogu.gitopsbuildlib.docker.DockerWrapper
import com.cloudogu.gitopsbuildlib.validation.GitopsTool
import com.cloudogu.gitopsbuildlib.validation.SourceType

abstract class Deployment {

    protected static String getKubectlImage() { 'lachlanevenson/k8s-kubectl:v1.19.3' }
    protected String extraResourcesFolder = ""

    static String getConfigDir() { '.config' }

    protected script
    protected Map gitopsConfig

    protected DockerWrapper dockerWrapper

    Deployment(def script, def gitopsConfig) {
        this.script = script
        this.gitopsConfig = gitopsConfig
        dockerWrapper = new DockerWrapper(this.script)
    }

    def create(String stage) {
        createFoldersAndCopyK8sResources(stage)
        createFileConfigmaps(stage)
        preValidation(stage)
        validate(stage)
        postValidation(stage)
    }

    abstract preValidation(String stage)
    abstract postValidation(String stage)


    def validate(String stage) {
        gitopsConfig.validators.each { validatorConfig ->
            GitopsTool gitopsTool = gitopsConfig.gitopsTool.toUpperCase()
            if (validatorConfig.value.validator.getSupportedGitopsTools().contains(gitopsTool)) {
                script.echo "Executing validator ${validatorConfig.key} for ${gitopsTool.name()}"
                validatorConfig.value.validator.getSupportedSourceTypes().each { sourceType ->
                    String targetDirectory = ''
                    if (sourceType.equals(SourceType.HELM)) {
                        targetDirectory = "${script.env.WORKSPACE}/.helmChartTempDir"
                    } else if (sourceType.equals(SourceType.PLAIN)) {
                        targetDirectory = "${stage}/${gitopsConfig.application}"
                    }
                    validatorConfig.value.validator.validate(validatorConfig.value.enabled, targetDirectory, validatorConfig.value.config, gitopsConfig)
                }
            }
        }
    }

    def createFoldersAndCopyK8sResources(String stage) {
        def sourcePath = gitopsConfig.deployments.sourcePath
        def application = gitopsConfig.application

        script.sh "mkdir -p ${stage}/${application}/${extraResourcesFolder}"
        script.sh "mkdir -p ${configDir}/"
        // copy extra resources like sealed secrets
        script.echo "Copying k8s payload from application repo to gitOps Repo: '${sourcePath}/${stage}/*' to '${stage}/${application}/${extraResourcesFolder}'"
        script.sh "cp -r ${script.env.WORKSPACE}/${sourcePath}/${stage}/* ${stage}/${application}/${extraResourcesFolder} || true"
        script.sh "cp ${script.env.WORKSPACE}/*.yamllint.yaml ${configDir}/ || true"
    }

    void createFileConfigmaps(String stage) {
        gitopsConfig.fileConfigmaps.each {
            if(stage in it['stage']) {
                String key = it['sourceFilePath'].split('/').last()
                script.writeFile file: "${stage}/${gitopsConfig.application}/generatedResources/${it['name']}.yaml", text: createConfigMap(key, "${script.env.WORKSPACE}/${gitopsConfig.deployments.sourcePath}/${it['sourceFilePath']}", it['name'], getNamespace(stage))
            }
        }
    }

    String createConfigMap(String key, String filePath, String name, String namespace) {
        String configMap = ""
        withDockerImage(kubectlImage) {
            String kubeScript = "KUBECONFIG=${writeKubeConfig()} kubectl create configmap ${name} " +
                "--from-file=${key}=${filePath} " +
                "--dry-run=client -o yaml -n ${namespace}"

            configMap = script.sh returnStdout: true, script: kubeScript
        }
        return configMap
    }

    void withDockerImage(String image, Closure body) {
        dockerWrapper.withDockerImage(image, body)
    }

    void withHelm(Closure body) {
        dockerWrapper.withHelm(body)
    }

    // Dummy kubeConfig, so we can use `kubectl --dry-run=client`
    String writeKubeConfig() {
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

    String getNamespace(String stage) {
        def namespace
        if (gitopsConfig.stages."${stage}".containsKey('namespace')) {
            namespace = gitopsConfig.stages."${stage}".namespace
        } else {
            namespace = stage
        }
        return namespace
    }
}
