package com.cloudogu.gitopsbuildlib.docker

class DockerWrapper {

    protected static String getHelmImage() { 'ghcr.io/cloudogu/helm:3.5.4-1' }

    protected def script

    DockerWrapper(def script) {
        this.script = script
    }

    void withDockerImage(String image, Closure body) {
        script.docker.image(image).inside(
            // Allow accessing WORKSPACE even when we are in a child dir (using "dir() {}")
            "${script.pwd().equals(script.env.WORKSPACE) ? '' : "-v ${script.env.WORKSPACE}:${script.env.WORKSPACE} "}" +
                // Avoid: "ERROR: The container started but didn't run the expected command"
                '--entrypoint=""'
        ) {
            body()
        }
    }

    void withHelm(Closure body) {
        script.cesBuildLib.Docker.new(script).image(helmImage).inside(
            "${script.pwd().equals(script.env.WORKSPACE) ? '' : "-v ${script.env.WORKSPACE}:${script.env.WORKSPACE}"}"
        ) {
            body()
        }
    }
}
