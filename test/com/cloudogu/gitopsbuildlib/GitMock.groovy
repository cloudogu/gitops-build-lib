package com.cloudogu.ces.cesbuildlib

import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class GitMock {
    List<String> actualArgs = new LinkedList<>()

    Git createMock() {
        Git gitMock = mock(Git.class)
        return gitMock
    }
}
