package com.cloudogu.gitopsbuildlib.scm

class SCMManager extends SCMProvider {
    protected String credentials
    protected String baseUrl
    protected String repositoryUrl


    SCMManager(def script) {
        super(script)
    }

    @Override
    void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl }

    @Override
    void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl }

    @Override
    void setCredentials(String credentialsId) { this.credentials = credentialsId }

    @Override
    String getRepositoryUrl() {
        return "${this.baseUrl}/repo/${this.repositoryUrl}"
    }

    @Override
    void createOrUpdatePullRequest(String stageBranch, String mainBranch, String title, String description) {
        script.cesBuildLib.SCMManager.new(script, this.baseUrl, this.credentials).createOrUpdatePullRequest(this.repositoryUrl, stageBranch, mainBranch, title, description)
    }
}
