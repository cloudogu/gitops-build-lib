package com.cloudogu.gitopsbuildlib

import com.cloudogu.gitopsbuildlib.validation.HelmKubeval
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat

class HelmKubevalTest {
    def scriptMock = new ScriptMock()
    def dockerMock = scriptMock.dockerMock
    def helmKubeval = new HelmKubeval(scriptMock.mock)

    @Test
    void 'is executed with defaults'() {
        helmKubeval.validate(
            'target',
            [image           : 'img',
            k8sSchemaVersion: '1.5'],
            [helm: [
                repoUrl: 'chartRepo'
            ]]
        )
        assertThat(dockerMock.actualImages[0]).isEqualTo('img')
        assertThat(scriptMock.actualShArgs[0]).isEqualTo('git clone chartRepo target/chart || true')
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('helm kubeval target/chart -v 1.5 --strict')
        assertThat(scriptMock.actualShArgs[2]).isEqualTo('rm -rf target/chart')

    }

    @Test
    void 'is not executed on plain deployment'() {
        helmKubeval.validate(
            'target',
            [image           : 'img',
             k8sSchemaVersion: '1.5'],
            [plain: []]
        )
        assertThat(dockerMock.actualImages[0]).isEqualTo(null)
        assertThat(scriptMock.actualShArgs[0]).isEqualTo(null)
    }

    @Test
    void 'no git clone on '
}
