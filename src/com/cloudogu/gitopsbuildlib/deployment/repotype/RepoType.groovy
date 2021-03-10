package com.cloudogu.gitopsbuildlib.deployment.repotype

abstract class RepoType {

    protected static String getHelmImage() { 'ghcr.io/cloudogu/helm:3.4.1-1' }

    protected script

    RepoType(def script) {
        this.script = script
    }

    abstract createHelmRelease(Map helmConfig, String application, String namespace, String valuesFile)
    abstract mergeValues(Map helmConfig, String[] files)

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
        return values
    }

    void withHelm(Closure body) {
        script.cesBuildLib.Docker.new(script).image(helmImage).inside(
            "${script.pwd().equals(script.env.WORKSPACE) ? '' : "-v ${script.env.WORKSPACE}:${script.env.WORKSPACE}"}"
        ) {
            body()
        }
    }
}
