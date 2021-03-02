package com.cloudogu.gitopsbuildlib.validation

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
    void validate(String targetDirectory, Map config, Map deployments) {
        withDockerImage(config.image) {
            script.sh "yamllint " +
                "${config.profile ? "-d ${config.profile} " : ''}" +
                '-f standard ' + // non-colored for CI-server  
                "${targetDirectory}"
        }
    }

}
