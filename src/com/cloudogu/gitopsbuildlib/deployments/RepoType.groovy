package com.cloudogu.gitopsbuildlib.deployments

abstract class RepoType {

    protected static String getKubectlImage() { 'lachlanevenson/k8s-kubectl:v1.19.3' }
    protected static String getHelmImage() { 'ghcr.io/cloudogu/helm:3.4.1-1' }

    protected String stage
    protected Map gitopsConfig

    RepoType(String stage, Map gitopsConfig) {
        this.stage = stage
        this.gitopsConfig = gitopsConfig
    }

    abstract protected generateFoldersAndFiles()
}
