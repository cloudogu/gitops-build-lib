package com.cloudogu.ces.cesbuildlib

import static org.mockito.Mockito.mock

class GitMock {
    List<String> actualArgs = new LinkedList<>()

    Git createMock() {
        Git gitMock = mock(Git.class)
        return gitMock
    }
}
