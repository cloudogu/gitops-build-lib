package com.cloudogu.gitopsbuildlib

import com.cloudogu.gitopsbuildlib.validation.Yamllint
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat

class YamllintTest {
    def scriptMock = new ScriptMock()
    def dockerMock = scriptMock.dockerMock
    def yamllint = new Yamllint(scriptMock.mock)

    @Test
    void 'is executed with defaults'() {
        yamllint.validate(
            'target',
            [image  : 'img',
            profile: 'pro'],
            [plain: []]
        )
        assertThat(dockerMock.actualImages[0]).isEqualTo('img')
        assertThat(scriptMock.actualShArgs[0]).isEqualTo(
            'yamllint -d pro -f standard target'
        )
    }

    @Test
    void 'is executed without profile'() {
        yamllint.validate(
            'target',
            [image: 'img'],
            [plain: []]
        )
        assertThat(scriptMock.actualShArgs[0]).isEqualTo(
            'yamllint -f standard target'
        )
    }
}
