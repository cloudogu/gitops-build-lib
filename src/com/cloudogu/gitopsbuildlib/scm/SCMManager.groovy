package com.cloudogu.gitopsbuildlib.scm

class SCMManager extends SCMProvider {

    SCMManager(def script) {
        super(script)
    }

    @Override
    void setBaseUrl(String baseUrl) { super.baseUrl = baseUrl }

    @Override
    void setRepositoryUrl(String repositoryUrl) { super.repositoryUrl = repositoryUrl }

    @Override
    void setCredentials(String credentialsId) { super.credentials = credentialsId }

    @Override
    String getRepositoryUrl() {
        return "${super.baseUrl}/repo/${super.repositoryUrl}"
    }

    @Override
    void createOrUpdatePullRequest(String stageBranch, String mainBranch, String title, String description) {
        def scmm = script.cesBuildLib.SCMManager.new(this, super.baseUrl, super.credentials)
        scmm.createOrUpdatePullRequest(this.repositoryUrl, stageBranch, mainBranch, title, description)
    }
}
