package com.cloudogu.gitopsbuildlib.scm

abstract class SCMProvider {

    protected def script
    protected String credentials = ''

    // scm
    protected String baseUrl
    protected String repositoryUrl



    SCMProvider(def script) {
        this.script = script
    }

    // TODO: check if implementation has to be done within the implementation itself or can be achieved like this in the base class
    void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl }

    void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl }

    void setCredentials(String credentialsId) { this.credentials = credentialsId }


    abstract String getRepositoryUrl()

    abstract void createOrUpdatePullRequest(String pullRequestRepo, String stageBranch, String mainBranch, String title, String description)
}
