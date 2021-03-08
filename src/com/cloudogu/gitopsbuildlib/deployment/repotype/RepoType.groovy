package com.cloudogu.gitopsbuildlib.deployment.repotype

abstract class RepoType {

    protected static String getKubectlImage() { 'lachlanevenson/k8s-kubectl:v1.19.3' }
    protected static String getHelmImage() { 'ghcr.io/cloudogu/helm:3.4.1-1' }

    protected script
    protected Map gitopsConfig

    RepoType(def script, Map gitopsConfig) {
        this.script = script
        this.gitopsConfig = gitopsConfig
    }

    abstract createHelmDeployment(String stage)
}
