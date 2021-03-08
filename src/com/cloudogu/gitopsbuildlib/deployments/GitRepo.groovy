package com.cloudogu.gitopsbuildlib.deployments

class GitRepo extends RepoType{

    GitRepo(def script, Map gitopsConfig) {
        super(script, gitopsConfig)
    }

    @Override
    protected generateFoldersAndFiles(String stage) {
        def helmConfig = gitopsConfig.deployments.helm
        def application = gitopsConfig.application
        def sourcePath = gitopsConfig.deployments.sourcePath





        script.sh "mkdir -p ${stage}/${application}/"

        //TODO extraresources kopieren?
        script.echo "Copying extra resources from application repo to gitOps Repo: '${sourcePath}/${stage}/*' to '${stage}/${application}'"
        script.sh "cp -a ${script.env.WORKSPACE}/${sourcePath}/${stage}/. ${stage}/${application}/ || true"






        // writing the merged-values.yaml via writeYaml into a file has the advantage, that it gets formatted as valid yaml
        // This makes it easier to read in and indent for the inline use in the helmRelease.
        // It enables us to reuse the `fileToInlineYaml` function, without writing a complex formatting logic.
        script.writeFile file: "${stage}/${application}/mergedValues.yaml", text: mergeValues(helmConfig.repoUrl, ["${script.env.WORKSPACE}/${sourcePath}/values-${stage}.yaml", "${script.env.WORKSPACE}/${sourcePath}/values-shared.yaml"] as String[])

        updateYamlValue("${stage}/${application}/mergedValues.yaml", helmConfig)
        script.writeFile file: "${stage}/${application}/helmRelease.yaml", text: createHelmRelease(helmConfig, application, "fluxv1-${stage}", "${stage}/${application}/mergedValues.yaml")
        // since the values are already inline (helmRelease.yaml) we do not need to commit them into the gitops repo
        script.sh "rm ${stage}/${application}/mergedValues.yaml"
    }

    private void updateYamlValue(String yamlFilePath, Map helmConfig) {
        def data = script.readYaml file: yamlFilePath
        helmConfig.updateValues.each {
            String[] paths = it["fieldPath"].split("\\.")
            def _tmp = data
            paths.eachWithIndex { String p, int i ->
                def tmp = _tmp.get(p)
                if (i == paths.length - 1 && tmp != null) {
                    _tmp.put(p, it["newValue"])
                }
                _tmp = tmp
            }
        }
        script.writeYaml file: yamlFilePath, data: data, overwrite: true
    }

    private String createHelmRelease(Map helmConfig, String application, String namespace, String valuesFile) {
        def values = fileToInlineYaml(valuesFile)
        return """apiVersion: helm.fluxcd.io/v1
kind: HelmRelease
metadata:
  name: ${application}
  namespace: ${namespace}
  annotations:
    fluxcd.io/automated: "false"
spec:
  releaseName: ${application}
  chart:
    git: ${helmConfig.repoUrl}
    ref: ${helmConfig.version}
    path: .
  values:
    ${values}
"""
    }

    private String fileToInlineYaml(String fileContents) {
        String values = ""
        String indent = "    "
        def fileContent = script.readFile fileContents
        values += fileContent.split("\\n").join("\n" + indent)
        return values
    }

    private String mergeValues(String chart, String[] files) {
        String merge = ""
        String _files = ""
        files.each {
            _files += "-f $it "
        }

        script.sh "git clone ${chart} ${script.env.WORKSPACE}/chart || true"

        withHelm {
            String helmScript = "helm values ${script.env.WORKSPACE}/chart ${_files}"
            merge = script.sh returnStdout: true, script: helmScript
        }

        script.sh "rm -rf ${script.env.WORKSPACE}/chart || true"

        return merge
    }

    private void withHelm(Closure body) {
        script.cesBuildLib.Docker.new(script).image(helmImage).inside(
            "${script.pwd().equals(script.env.WORKSPACE) ? '' : "-v ${script.env.WORKSPACE}:${script.env.WORKSPACE}"}"
        ) {
            body()
        }
    }
}
