package com.cloudogu.gitopsbuildlib.deployment.helm.repotype

abstract class RepoType {

    protected static String getHelmImage() { 'ghcr.io/cloudogu/helm:3.5.4-1' }

    protected script

    RepoType(def script) {
        this.script = script
    }

    abstract mergeValues(Map helmConfig, String[] files)

    void withHelm(Closure body) {
        script.cesBuildLib.Docker.new(script).image(helmImage).inside(
            "${script.pwd().equals(script.env.WORKSPACE) ? '' : "-v ${script.env.WORKSPACE}:${script.env.WORKSPACE}"}"
        ) {
            body()
        }
    }

    protected String valuesFilesWithParameter(String[] valuesFiles) {
        String valuesFilesWithParameter = ""
        valuesFiles.each {
            valuesFilesWithParameter += "-f $it "
        }
        return valuesFilesWithParameter
    }
}
