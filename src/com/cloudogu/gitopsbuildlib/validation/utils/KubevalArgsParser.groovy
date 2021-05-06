package com.cloudogu.gitopsbuildlib.validation.utils

class KubevalArgsParser {

    private boolean strict = true
    private boolean ignoreMissingSchemas = true
    private List<String> skipKinds = []

    String parse(Map validatorConfig) {

        String args = ''

        args += parseStrict(validatorConfig)
        args += parseIgnoreMissingSchemas(validatorConfig)
        args += parseSkipKinds(validatorConfig)

        return args
    }

    private String parseStrict(Map validatorConfig) {
        String strictArgs = ''
        if(validatorConfig.containsKey('strict')) {
            strict = validatorConfig.strict
        }
        if(strict) {
            strictArgs += " --strict"
        }
        return strictArgs
    }

    private String parseIgnoreMissingSchemas(Map validatorConfig) {
        String ignoreMissingSchemasArgs = ''
        if(validatorConfig.containsKey('ignoreMissingSchemas')) {
            ignoreMissingSchemas = validatorConfig.ignoreMissingSchemas
        }
        if(ignoreMissingSchemas) {
            ignoreMissingSchemasArgs += " --ignore-missing-schemas"
        }
        return ignoreMissingSchemasArgs
    }

    private String parseSkipKinds(Map validatorConfig) {
        String skipKindsArgs = ''
        if(validatorConfig.containsKey('skipKinds') && validatorConfig.skipKinds) {
            skipKinds = validatorConfig.skipKinds
        }
        if(!skipKinds.isEmpty()) {
            skipKindsArgs += " --skip-kinds ".concat(skipKinds.join(","))
        }
        return skipKindsArgs
    }
}
