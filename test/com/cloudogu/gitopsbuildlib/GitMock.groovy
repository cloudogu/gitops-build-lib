package com.cloudogu.gitopsbuildlib

import com.cloudogu.ces.cesbuildlib.Git

import static org.mockito.Mockito.mock

class GitMock {

    Git createMock() {
        Git gitMock = mock(Git.class)
        return gitMock
    }
}
