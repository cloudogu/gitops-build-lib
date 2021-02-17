package com.cloudogu.gitopsbuildlib

import com.cloudbees.groovy.cps.NonCPS

class ValidateResources {

    String getHelmImage() { 'ghcr.io/cloudogu/helm:3.4.1-1'}
    String getYamlLintImage() { 'cytopia/yamllint:1.25' }
    String getK8sVersion() { '1.18.1 '}
    String targetDirectory
    String configFile
    def script
    def cesBuildLib

    ValidateResources(def script, String targetDirectory, String configFile, def cesBuildLib) {
        this.script = script
        this.targetDirectory = targetDirectory
        this.configFile = configFile
        this.cesBuildLib = cesBuildLib
    }

    void start() {
        validateK8sRessources(targetDirectory, k8sVersion)
        validateYamlResources(configFile, targetDirectory)
    }

// Validates all yaml-resources within the target-directory against the specs of the given k8s version
    private void validateK8sRessources(String targetDirectory, String k8sVersion) {
        withDockerImage(helmImage) {
            script.sh "kubeval -d ${targetDirectory} -v ${k8sVersion} --strict"
        }
    }

    private void validateYamlResources(String configFile, String targetDirectory) {
        withDockerImage(yamlLintImage) {
            script.sh "yamllint -c ${configFile} ${targetDirectory}"
        }
    }

    @NonCPS
    private void withDockerImage(String image, Closure body) {
        def docker = cesBuildLib.Docker.new(this)
        docker.image(image)
        // Allow accessing WORKSPACE even when we are in a child dir (using "dir() {}")
                .inside("${script.pwd().equals(script.env.WORKSPACE) ? '' : "-v ${script.env.WORKSPACE}:${script.env.WORKSPACE}"}") {
                    body()
                }
    }
}
