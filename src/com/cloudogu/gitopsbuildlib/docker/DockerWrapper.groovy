package com.cloudogu.gitopsbuildlib.docker

class DockerWrapper {
    protected def script

    DockerWrapper(def script) {
        this.script = script
    }

    void withDockerImage(def imageConfig, Closure body) {
        // imageConfig can either be a Map or a String, depending on the old or the new format if this field
        // The old format was a String containing an image url. The new one is a map with an image url and optional credentials
        if (imageConfig instanceof Map) {
            if (imageConfig.containsKey('credentialsId') && imageConfig.credentialsId) {
                def registryUrl = getRegistryUrlFromImage(imageConfig.image)
                script.docker.withRegistry("https://${registryUrl}", imageConfig.credentialsId) {
                    runDockerImage(imageConfig.image, body)
                }
            } else {
                runDockerImage(imageConfig.image, body)
            }
        } else {
            // When imageConfig is a String
            runDockerImage(imageConfig, body)
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
