package com.cloudogu.gitopsbuildlib.scm

abstract class SCMProvider {

    protected script

    SCMProvider(def script) {
        this.script = script
    }

    abstract void setBaseUrl(String baseUrl)

    abstract void setRepositoryUrl(String repositoryUrl)

    abstract void setCredentials(String credentialsId)

    abstract String getRepositoryUrl()

    abstract void createOrUpdatePullRequest(String stageBranch, String mainBranch, String title, String description)
}
