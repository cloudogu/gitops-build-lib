package com.cloudogu.gitopsbuildlib.deployment.helm.repotype

import com.cloudogu.gitopsbuildlib.docker.DockerWrapper

abstract class RepoType {

    protected script
    protected DockerWrapper dockerWrapper

    RepoType(def script) {
        this.script = script
        dockerWrapper = new DockerWrapper(script)
    }

    abstract mergeValues(Map helmConfig, String[] files)

    void withHelm(Closure body) {
        dockerWrapper.withHelm {
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
