package com.cloudogu.ces.cesbuildlib

import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class DockerMock {

    List<String> actualInsideArgs = new LinkedList<>()
    List<String> actualImages = new LinkedList<>()
    
    Docker createMock() {
        Docker dockerMock = mock(Docker.class)
        Docker.Image imageMock = mock(Docker.Image.class)
        when(dockerMock.image(anyString())).thenAnswer(new Answer<Object>() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                actualImages += invocation.getArgument(0)
                return imageMock
            }
        })
        
        when(imageMock.mountJenkinsUser()).thenReturn(imageMock)
        when(imageMock.mountJenkinsUser(anyBoolean())).thenReturn(imageMock)
        when(imageMock.mountDockerSocket()).thenReturn(imageMock)
        when(imageMock.mountDockerSocket(anyBoolean())).thenReturn(imageMock)
        when(imageMock.inside(anyString(), any())).thenAnswer(new Answer<Object>() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                actualInsideArgs += invocation.getArgument(0)
                Closure closure = invocation.getArgument(1)
                closure.call()
            }
        })
        return dockerMock
    }
}
