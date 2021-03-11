package com.cloudogu.gitopsbuildlib.deployment

abstract class Deployment {

    protected static String getKubectlImage() { 'lachlanevenson/k8s-kubectl:v1.19.3' }

    static String getConfigDir() { '.config' }

    protected script
    protected Map gitopsConfig

    Deployment(def script, def gitopsConfig) {
        this.script = script
        this.gitopsConfig = gitopsConfig
    }

    def create(String stage) {
        createFoldersAndCopyK8sResources(stage)
        createFileConfigmaps(stage)
        createPreValidation(stage)
        validate(stage)
        createPostValidation(stage)
    }

    abstract createPreValidation(String stage)
    abstract createPostValidation(String stage)

    def validate(String stage) {
        gitopsConfig.validators.each { validatorConfig ->
            script.echo "Executing validator ${validatorConfig.key}"
            validatorConfig.value.validator.validate(validatorConfig.value.enabled, "${stage}/${gitopsConfig.application}", validatorConfig.value.config, gitopsConfig.deployments)
        }
    }

    def createFoldersAndCopyK8sResources(String stage) {
        def sourcePath = gitopsConfig.deployments.sourcePath
        def application = gitopsConfig.application

        script.sh "mkdir -p ${stage}/${application}/${sourcePath}/"
        script.sh "mkdir -p ${configDir}/"
        // copy extra resources like sealed secrets
        script.echo "Copying k8s payload from application repo to gitOps Repo: '${sourcePath}/${stage}/*' to '${stage}/${application}/${sourcePath}'"
        script.sh "cp -r ${script.env.WORKSPACE}/${sourcePath}/${stage}/* ${stage}/${application}/${sourcePath}/ || true"
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
        withKubectl {
            String kubeScript = "KUBECONFIG=${writeKubeConfig()} kubectl create configmap ${name} " +
                "--from-file=${key}=${filePath} " +
                "--dry-run=client -o yaml -n ${namespace}"

            configMap = script.sh returnStdout: true, script: kubeScript
        }
        return configMap
    }

    void withKubectl(Closure body) {
        script.cesBuildLib.Docker.new(script).image(kubectlImage)
        // Allow accessing WORKSPACE even when we are in a child dir (using "dir() {}")
            .inside("${script.pwd().equals(script.env.WORKSPACE) ? '' : "-v ${script.env.WORKSPACE}:${script.env.WORKSPACE}"}") {
                body()
            }
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
