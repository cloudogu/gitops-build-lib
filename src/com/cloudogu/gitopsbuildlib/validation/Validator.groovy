package com.cloudogu.gitopsbuildlib.validation

abstract class Validator {

    protected script

    Validator(def script) {
        this.script = script
    }

    void validate(boolean enabled, String targetDirectory, Map config, Map deployments) {
        if (enabled) {
            validate(targetDirectory, config, deployments)
        } else {
            script.echo "Skipping validator ${this.getClass().getSimpleName()} because it is configured as enabled=false"
        }
    }

    abstract protected void validate(String targetDirectory, Map config, Map deployments)

    protected void withDockerImage(String image, Closure body) {
        script.docker.image(image).inside(
            // Allow accessing WORKSPACE even when we are in a child dir (using "dir() {}")
            "${script.pwd().equals(script.env.WORKSPACE) ? '' : "-v ${script.env.WORKSPACE}:${script.env.WORKSPACE} "}" +
                // Avoid: "ERROR: The container started but didn't run the expected command"
                '--entrypoint=""'
        ) {
            body()
        }
    }
}
