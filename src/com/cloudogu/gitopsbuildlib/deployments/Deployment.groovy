package com.cloudogu.gitopsbuildlib.deployments

interface Deployment {

    def createApplicationFolders(String stage, Map gitopsConfig)
    def update(String stage, Map gitopsConfig)
}
