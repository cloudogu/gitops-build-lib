package com.cloudogu.gitopsbuildlib.scm

class SCMManager extends SCMProvider {

    SCMManager(def script) {
        super(script)
    }

    @Override
    String getRepositoryUrl() {
        return "${this.baseUrl}/repo/${this.repository}"
    }

    @Override
    void createOrUpdatePullRequest(String pullRequestRepo, String stageBranch, String mainBranch, String title, String description) {
        def scmm = script.cesBuildLib.SCMManager.new(this, "scmmPullRequestBaseUrl", this.credentials)
        scmm.createOrUpdatePullRequest(pullRequestRepo, stageBranch, mainBranch, title, description)
    }
}
