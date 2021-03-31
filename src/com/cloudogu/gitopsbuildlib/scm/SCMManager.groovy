package com.cloudogu.gitopsbuildlib.scm

class SCMManager extends SCMProvider {

    SCMManager(def script) {
        super(script)
    }


    // TODO: refactor!
    @Override
    String getRepositoryUrl() {
        if (!this.baseUrl || this.repositoryUrl)
            return ""
        else
            return "${this.baseUrl}/repo/${this.repositoryUrl}"
    }

    // TODO: PR-Url could be something different dependant on implementation and is not passed in anymore via gitopsconfig map
    @Override
    void createOrUpdatePullRequest(String pullRequestRepo, String stageBranch, String mainBranch, String title, String description) {
        def scmm = script.cesBuildLib.SCMManager.new(this, "scmmPullRequestBaseUrl", this.credentials)
        scmm.createOrUpdatePullRequest(pullRequestRepo, stageBranch, mainBranch, title, description)
    }
}
