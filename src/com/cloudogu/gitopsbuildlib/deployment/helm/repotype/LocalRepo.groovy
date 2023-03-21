package com.cloudogu.gitopsbuildlib.deployment.helm.repotype

class LocalRepo extends RepoType {

    LocalRepo(def script) {
        super(script)
    }

    @Override
    void prepareRepo(Map gitopsConfig, String helmChartTempDir, String chartRootDir) {
        // Nothing to prepare for a local chart
    }

    @Override
    String getChartPath(Map gitopsConfig, String helmChartTempDir, String chartRootDir) {
        def chartPath = gitopsConfig.deployments.helm.chartPath
        // Use absolute paths, so e.g. helm values works in .configRepoTempDir
        if (!chartRootDir.contains(script.env.WORKSPACE)) {
            chartPath = "${script.env.WORKSPACE}/${chartPath}"
        }
        return "${chartPath}"
    }
}
