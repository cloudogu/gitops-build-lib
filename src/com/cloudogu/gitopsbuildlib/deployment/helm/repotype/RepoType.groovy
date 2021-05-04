package com.cloudogu.gitopsbuildlib.deployment.helm.repotype

import com.cloudogu.gitopsbuildlib.docker.DockerWrapper

abstract class RepoType {

    protected script
    protected DockerWrapper dockerWrapper

    RepoType(def script) {
        this.script = script
        dockerWrapper = new DockerWrapper(script)
    }

    abstract void prepareRepo(Map helmConfig, String helmChartTempDir, String chartRootDir)

    void withHelm(Closure body) {
        dockerWrapper.withHelm(body)
    }
}
