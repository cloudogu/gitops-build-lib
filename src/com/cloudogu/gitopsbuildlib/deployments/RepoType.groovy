package com.cloudogu.gitopsbuildlib.deployments

abstract class RepoType {

    protected static String getKubectlImage() { 'lachlanevenson/k8s-kubectl:v1.19.3' }
    protected static String getHelmImage() { 'ghcr.io/cloudogu/helm:3.4.1-1' }

    protected Map gitopsConfig

    RepoType(Map gitopsConfig) {
        this.gitopsConfig = gitopsConfig
    }

    abstract protected generateFoldersAndFiles(String stage)
}
