package com.cloudogu.gitopsbuildlib.validation

import com.cloudogu.gitopsbuildlib.ScriptMock
import com.cloudogu.gitopsbuildlib.validation.Kubeval
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat

class KubevalTest {
    def scriptMock = new ScriptMock()
    def dockerMock = scriptMock.dockerMock
    def kubeval = new Kubeval(scriptMock.mock)

    @Test
    void 'is executed with defaults'() {
        kubeval.validate(
            'target',
            [
                k8sSchemaVersion: '1.5'
            ],
            [
                sourcePath: 'k8s',
                destinationRootPath: '.',
                plain: []
            ]
        )
        assertThat(scriptMock.actualShArgs[0]).isEqualTo(
            'kubeval -d target -v 1.5 --strict --ignore-missing-schemas'
        )
    }
}
