package com.cloudogu.gitopsbuildlib.deployment

import com.cloudogu.gitopsbuildlib.docker.DockerWrapper

abstract class Deployment {

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
    abstract validate(String stage)

    def createFoldersAndCopyK8sResources(String stage) {
        def sourcePath = gitopsConfig.deployments.sourcePath
        def destinationPath = getDestinationFolder(getFolderStructureStrategy(), stage)

        script.sh "mkdir -p ${destinationPath}/${extraResourcesFolder}"
        script.sh "mkdir -p ${configDir}/"
        // copy extra resources like sealed secrets
        script.echo "Copying k8s payload from application repo to gitOps Repo: '${sourcePath}/${stage}/*' to '${destinationPath}/${extraResourcesFolder}'"
        script.sh "cp -r ${script.env.WORKSPACE}/${sourcePath}/${stage}/* ${destinationPath}/${extraResourcesFolder} || true"
        script.sh "cp ${script.env.WORKSPACE}/*.yamllint.yaml ${configDir}/ || true"
    }

    void createFileConfigmaps(String stage) {
        def destinationPath = getDestinationFolder(getFolderStructureStrategy(), stage)

        gitopsConfig.fileConfigmaps.each {
            if (stage in it['stage']) {
                String key = it['sourceFilePath'].split('/').last()
                script.writeFile file: "${destinationPath}/generatedResources/${it['name']}.yaml", text: createConfigMap(key, "${script.env.WORKSPACE}/${gitopsConfig.deployments.sourcePath}/${it['sourceFilePath']}", it['name'], getNamespace(stage))
            }
        }
    }

    String createConfigMap(String key, String filePath, String name, String namespace) {
        String configMap = ""
        withDockerImage(gitopsConfig.buildImages.kubectl) {
            String kubeScript = "kubectl create configmap ${name} " +
                "--from-file=${key}=${filePath} " +
                "--dry-run=client -o yaml -n ${namespace}"

            configMap = script.sh returnStdout: true, script: kubeScript
        }
        return configMap
    }

    void withDockerImage(def imageConfig, Closure body) {
        dockerWrapper.withDockerImage(imageConfig, body)
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

    String getDestinationFolder(FolderStructureStrategy folderStructureStrategy, String stage) {

        def destinationRootPath = gitopsConfig.deployments.destinationRootPath

        if (destinationRootPath == ".") {
            destinationRootPath = ""
        } else {
            if (!destinationRootPath.endsWith("/")) {
                destinationRootPath = destinationRootPath + "/"
            }
        }

        switch (folderStructureStrategy) {
            case FolderStructureStrategy.GLOBAL_ENV:
                return "${destinationRootPath}${stage}/${gitopsConfig.application}"
            case FolderStructureStrategy.ENV_PER_APP:
                return "${destinationRootPath}${gitopsConfig.application}/${stage}"
            default:
                return null
        }
    }

    protected GitopsTool getGitopsTool() {
        // Already asserted in deployViaGitOps
        GitopsTool.get(gitopsConfig.gitopsTool)
    }

    protected FolderStructureStrategy getFolderStructureStrategy() {
        // Already asserted in deployViaGitOps
        FolderStructureStrategy.get(gitopsConfig.folderStructureStrategy)
    }
}
