package com.cloudogu.gitopsbuildlib.deployment.helm.helmrelease

abstract class HelmRelease {

    protected script

    HelmRelease(def script) {
        this.script = script
    }

    abstract String create(Map gitopsConfig, String namespace, String mergedValuesFile)

    String fileToInlineYaml(String fileContents) {
        String values = ""
        String indent = "    "
        String fileContent = script.readFile fileContents
        fileContent.split("\n").each { line ->
            if(line.size() > 0) {
                values += indent + line + "\n"
            } else {
                values += line + "\n"
            }
        }
        // remove unnecessary last blank line
        return values.substring(0, values.lastIndexOf('\n'))
    }
}
