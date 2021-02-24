package com.cloudogu.gitopsbuildlib

/** Queries and stores info about current repo and HEAD commit */
class GitRepo {

    static GitRepo create(git) {
        // Constructors can't be used in Jenkins pipelines due to CPS
        // https://www.jenkins.io/doc/book/pipeline/cps-method-mismatches/#constructors
        return new GitRepo(git.commitAuthorName, git.commitAuthorEmail, git.commitHashShort, git.commitMessage, git.repositoryUrl)
    }

    GitRepo(String authorName, String authorEmail, String commitHash, String commitMessage, String repositoryUrl) {
        this.authorName = authorName
        this.authorEmail = authorEmail
        this.commitHash = commitHash
        this.commitMessage = commitMessage
        this.repositoryUrl = repositoryUrl
    }

    final String authorName
    final String authorEmail
    final String commitHash
    final String commitMessage
    final String repositoryUrl
}
