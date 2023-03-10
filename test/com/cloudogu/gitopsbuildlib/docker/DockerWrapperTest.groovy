package com.cloudogu.gitopsbuildlib.docker

import com.cloudogu.gitopsbuildlib.ScriptMock
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat 

class DockerWrapperTest {

    public static final String EXPECTED_IMAGE = 'ghcr.io/cloudogu/helm:3.11.1-2'
    
    def scriptMock = new ScriptMock()
    
    def dockerWrapper = new DockerWrapper(scriptMock.mock)
    
    def imageConfigMap = [
        image: EXPECTED_IMAGE,
    ]
    def imageConfigMapWithCredentials = [
        image: EXPECTED_IMAGE,
        credentialsId: 'myCredentials'
    ]
    def imageConfigString = EXPECTED_IMAGE
    
    @Test
    void 'works with imageConfig string'() {

        dockerWrapper.withDockerImage(imageConfigString) {
        }
        assertThat(scriptMock.dockerMock.actualImages[0]).isEqualTo(EXPECTED_IMAGE)
    }
    
    @Test
    void 'works with imageConfig Map'() {
        dockerWrapper.withDockerImage(imageConfigMap) {
        }
        assertThat(scriptMock.dockerMock.actualImages[0]).isEqualTo(EXPECTED_IMAGE)
    }
    
    @Test
    void 'works with imageConfig Map with Credentials'() {
        dockerWrapper.withDockerImage(imageConfigMapWithCredentials) {
        }
        assertThat(scriptMock.dockerMock.actualImages[0]).isEqualTo(EXPECTED_IMAGE)
        assertThat(scriptMock.dockerMock.actualRegistryArgs[0]).isEqualTo('https://ghcr.io/cloudogu')
        assertThat(scriptMock.dockerMock.actualRegistryArgs[1]).isEqualTo('myCredentials')
    }
}
