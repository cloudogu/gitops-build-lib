package com.cloudogu.gitopsbuildlib.scm

class SCMManager extends SCMProvider {

    SCMManager(def script) {
        super(script)
    }

    @Override
    String getRepositoryUrl() {
        if (!this.baseUrl || this.repositoryUrl)
            return ""
        else
            return "${this.baseUrl}/repo/${this.repositoryUrl}"
    }

    @Override
    void createOrUpdatePullRequest(String pullRequestRepo, String stageBranch, String mainBranch, String title, String description) {
        def scmm = script.cesBuildLib.SCMManager.new(this, "scmmPullRequestBaseUrl", this.credentials)
        scmm.createOrUpdatePullRequest(pullRequestRepo, stageBranch, mainBranch, title, description)
    }
}
