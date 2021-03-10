package com.cloudogu.gitopsbuildlib.deployments

interface Deployment {

    def prepareApplicationFolders(String stage, Map gitopsConfig)
    def update(String stage, Map gitopsConfig)
}
