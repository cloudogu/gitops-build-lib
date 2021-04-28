package com.cloudogu.gitopsbuildlib.validation.utils

import org.junit.jupiter.api.Test
import static org.assertj.core.api.Assertions.assertThat

class KubevalArgsParserTest {

    @Test
    void 'no config yields correct defaults'() {
        ArgsParser argsParser = new KubevalArgsParser()
        String output = argsParser.parse([:])

        assertThat(output).isEqualTo(' --strict --ignore-missing-schemas')
    }

    @Test
    void 'setting all args yields correct output'() {
        ArgsParser argsParser = new KubevalArgsParser()
        String output = argsParser.parse([
            strict: true,
            ignoreMissingSchemas: true,
            skipKinds: ['kind1', 'kind2']
        ])

        assertThat(output).isEqualTo(' --strict --ignore-missing-schemas --skip-kinds kind1,kind2')
    }

    @Test
    void 'disabling args yields correct output'() {
        ArgsParser argsParser = new KubevalArgsParser()
        String output = argsParser.parse([
            strict: false,
            ignoreMissingSchemas: false,
            skipKinds: []
        ])

        assertThat(output).isEqualTo('')
    }

    @Test
    void 'mixing config yields correct output'() {
        ArgsParser argsParser = new KubevalArgsParser()
        String output = argsParser.parse([
            strict: false,
            skipKinds: ['kind1']
        ])

        assertThat(output).isEqualTo(' --ignore-missing-schemas --skip-kinds kind1')
    }
}
