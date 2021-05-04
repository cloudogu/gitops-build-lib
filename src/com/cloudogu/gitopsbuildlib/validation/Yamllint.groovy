package com.cloudogu.gitopsbuildlib.validation

import com.cloudogu.gitopsbuildlib.deployment.GitopsTool
import com.cloudogu.gitopsbuildlib.deployment.SourceType

/**
 * Checks for correct YAML syntax using yamllint
 *
 * If you want to use your own yamllint configuration your best bet is the following:
 * ".yamllint, .yamllint.yaml or .yamllint.yml in the current working directory"
 * See: https://yamllint.readthedocs.io/en/stable/configuration.html
 */
class Yamllint extends Validator {

    Yamllint(def script) {
        super(script)
    }

    @Override
    void validate(String targetDirectory, Map validatorConfig, Map gitopsConfig) {
        withDockerImage(validatorConfig.image) {
            script.sh "yamllint " +
                "${validatorConfig.profile ? "-d ${validatorConfig.profile} " : ''}" +
                '-f standard ' + // non-colored for CI-server  
                "${targetDirectory}"
        }
    }

    @Override
    SourceType[] getSupportedSourceTypes() {
        return [SourceType.PLAIN]
    }

    @Override
    GitopsTool[] getSupportedGitopsTools() {
        return [GitopsTool.FLUX, GitopsTool.ARGO]
    }
}
