package com.cloudogu.gitopsbuildlib.docker

class DockerWrapper {
    protected def script

    DockerWrapper(def script) {
        this.script = script
    }

    void withDockerImage(def imageConfig, Closure body) {
        if(imageConfig.containsKey('registryCredentialsId') && imageConfig.registryCredentialsId) {
            def registryUrl = getRegistryUrlFromImage(imageConfig.image)
            script.docker.withRegistry("https://${registryUrl}", imageConfig.registryCredentialsId) {
                runDockerImage(imageConfig.image, body)
            }
        } else {
            runDockerImage(imageConfig.image, body)
        }
    }

    private String getRegistryUrlFromImage(String image) {
        int i = image.lastIndexOf('/')
        return  image.substring(0, i)
    }

    private void runDockerImage(String image, Closure body) {
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
