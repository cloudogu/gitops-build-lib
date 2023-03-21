package com.cloudogu.gitopsbuildlib.deployment.helm.repotype

class LocalRepo extends RepoType {

    LocalRepo(def script) {
        super(script)
    }

    @Override
    void prepareRepo(Map gitopsConfig, String helmChartTempDir, String chartRootDir) {
    }
}
