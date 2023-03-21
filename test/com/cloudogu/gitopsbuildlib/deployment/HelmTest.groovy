package com.cloudogu.gitopsbuildlib.deployment

import com.cloudogu.gitopsbuildlib.ScriptMock
import com.cloudogu.gitopsbuildlib.deployment.helm.Helm
import com.cloudogu.gitopsbuildlib.validation.HelmKubeval
import com.cloudogu.gitopsbuildlib.validation.Kubeval
import com.cloudogu.gitopsbuildlib.validation.Yamllint
import org.junit.jupiter.api.*

import static org.assertj.core.api.Assertions.assertThat

class HelmTest {

    def gitRepo = [
        sourcePath: 'k8s',
        destinationRootPath: '.',
        helm      : [
            repoType: 'GIT',
            repoUrl: 'repoUrl',
            chartPath: 'chartPath'
        ]
    ]

    def helmRepo = [
        sourcePath: 'k8s',
        destinationRootPath: '.',
        helm      : [
            repoType: 'HELM',
            repoUrl: 'repoUrl',
            chartName: 'chartName',
            version: '1.0'
        ]
    ]
    
    def localRepo = [
        sourcePath: 'k8s',
        destinationRootPath: '.',
        helm      : [
            repoType: 'LOCAL',
            chartPath: 'chart/path'
        ]
    ]

    private Map getGitopsConfig(Map deployment, String tool = 'FLUX') {
        return [
            application: 'app',
            gitopsTool: tool,
            stages: [
                staging: [
                    namespace: 'fluxv1-staging'
                ]
            ],
            folderStructureStrategy: 'GLOBAL_ENV',
            buildImages: [
                helm: [
                    image: 'helmImage',
                ],
                kubectl: [
                    image: 'kubectlImage'
                ]
            ],
            deployments: deployment,
            validators: [
                yamllint: [
                    validator: new Yamllint(scriptMock.mock),
                    enabled: true,
                    config: [
                        image: 'img'
                    ]
                ],
                kubeval: [
                    validator: new Kubeval(scriptMock.mock),
                    enabled: true,
                    config: [
                        image: 'img'
                    ]
                ],
                helmKubeval: [
                    validator: new HelmKubeval(scriptMock.mock),
                    enabled: true,
                    config: [
                        image: 'img'
                    ]
                ]
            ],
            fileConfigmaps: [
                [
                    name : "index",
                    sourceFilePath : "../index.html",
                    stage: ["staging"]
                ]
            ]
        ]
    }

    def scriptMock = new ScriptMock()
    def dockerMock = scriptMock.dockerMock
    def helmGit = new Helm(scriptMock.mock, getGitopsConfig(gitRepo))
    def helmHelm = new Helm(scriptMock.mock, getGitopsConfig(helmRepo))
    def helmLocal = new Helm(scriptMock.mock, getGitopsConfig(localRepo, 'ARGO'))

    @Test
    void 'creating helm release with git repo'() {
        helmGit.preValidation('staging')

        assertThat(dockerMock.actualImages[0]).contains('helmImage')
        assertThat(scriptMock.actualShArgs[0]).isEqualTo('helm dep update workspace/.helmChartTempDir/chart/chartPath')
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('[returnStdout:true, script:helm values workspace/.helmChartTempDir/chart/chartPath -f workspace/k8s/values-staging.yaml -f workspace/k8s/values-shared.yaml ]')
        assertThat(scriptMock.actualWriteFileArgs[0]).isEqualTo('[file:workspace/.helmChartTempDir/mergedValues.yaml, text:[helm dep update workspace/.helmChartTempDir/chart/chartPath, [returnStdout:true, script:helm values workspace/.helmChartTempDir/chart/chartPath -f workspace/k8s/values-staging.yaml -f workspace/k8s/values-shared.yaml ]]]')
        assertThat(scriptMock.actualWriteFileArgs[1]).isEqualTo('''[file:staging/app/applicationRelease.yaml, text:apiVersion: helm.fluxcd.io/v1
kind: HelmRelease
metadata:
  name: app
  namespace: fluxv1-staging
  annotations:
    fluxcd.io/automated: "false"
spec:
  releaseName: app
  chart:
    git: repoUrl
    ref: null
    path: chartPath
  values:
    ---
    #this part is only for PlainTest regarding updating the image name
    spec:
      template:
        spec:
          containers:
            - name: \'application\'
              image: \'oldImageName\'
    #this part is only for HelmTest regarding changing the yaml values
    to:
      be:
        changed: \'oldValue\'
]''')
    }

    @Test
    void 'creating helm release with helm repo'() {
        helmHelm.preValidation('staging')

        assertThat(dockerMock.actualImages[0]).contains('helmImage')
        assertThat(scriptMock.actualShArgs[0]).isEqualTo('helm repo add chartRepo repoUrl')
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('helm repo update')
        assertThat(scriptMock.actualShArgs[2]).isEqualTo('helm pull chartRepo/chartName --version=1.0 --untar --untardir=workspace/.helmChartTempDir/chart')
        assertThat(scriptMock.actualShArgs[3]).isEqualTo('[returnStdout:true, script:helm values workspace/.helmChartTempDir/chart/chartName -f workspace/k8s/values-staging.yaml -f workspace/k8s/values-shared.yaml ]')
        assertThat(scriptMock.actualWriteFileArgs[0]).isEqualTo('[file:workspace/.helmChartTempDir/mergedValues.yaml, text:[helm repo add chartRepo repoUrl, helm repo update, helm pull chartRepo/chartName --version=1.0 --untar --untardir=workspace/.helmChartTempDir/chart, [returnStdout:true, script:helm values workspace/.helmChartTempDir/chart/chartName -f workspace/k8s/values-staging.yaml -f workspace/k8s/values-shared.yaml ]]]')
        assertThat(scriptMock.actualWriteFileArgs[1]).isEqualTo('''[file:staging/app/applicationRelease.yaml, text:apiVersion: helm.fluxcd.io/v1
kind: HelmRelease
metadata:
  name: app
  namespace: fluxv1-staging
  annotations:
    fluxcd.io/automated: "false"
spec:
  releaseName: app
  chart:
    repository: repoUrl
    name: chartName
    version: 1.0
  values:
    ---
    #this part is only for PlainTest regarding updating the image name
    spec:
      template:
        spec:
          containers:
            - name: \'application\'
              image: \'oldImageName\'
    #this part is only for HelmTest regarding changing the yaml values
    to:
      be:
        changed: \'oldValue\'
]''')
    }

    @Test
    void 'creating helm release with git repo with ENV_PER_APP and other destinationRootPath'() {
        helmGit.gitopsConfig['folderStructureStrategy'] = 'ENV_PER_APP'
        helmGit.gitopsConfig['deployments']['destinationRootPath'] = 'apps'

        helmGit.preValidation('staging')

        assertThat(dockerMock.actualImages[0]).contains('helmImage')
        assertThat(scriptMock.actualShArgs[0]).isEqualTo('helm dep update workspace/.helmChartTempDir/chart/chartPath')
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('[returnStdout:true, script:helm values workspace/.helmChartTempDir/chart/chartPath -f workspace/k8s/values-staging.yaml -f workspace/k8s/values-shared.yaml ]')
        assertThat(scriptMock.actualWriteFileArgs[0]).isEqualTo('[file:workspace/.helmChartTempDir/mergedValues.yaml, text:[helm dep update workspace/.helmChartTempDir/chart/chartPath, [returnStdout:true, script:helm values workspace/.helmChartTempDir/chart/chartPath -f workspace/k8s/values-staging.yaml -f workspace/k8s/values-shared.yaml ]]]')
        assertThat(scriptMock.actualWriteFileArgs[1]).isEqualTo('''[file:apps/app/staging/applicationRelease.yaml, text:apiVersion: helm.fluxcd.io/v1
kind: HelmRelease
metadata:
  name: app
  namespace: fluxv1-staging
  annotations:
    fluxcd.io/automated: "false"
spec:
  releaseName: app
  chart:
    git: repoUrl
    ref: null
    path: chartPath
  values:
    ---
    #this part is only for PlainTest regarding updating the image name
    spec:
      template:
        spec:
          containers:
            - name: \'application\'
              image: \'oldImageName\'
    #this part is only for HelmTest regarding changing the yaml values
    to:
      be:
        changed: \'oldValue\'
]''')
    }

    @Test
    void 'creating helm release with helm repo with ENV_PER_APP and other destinationRootPath'() {
        helmHelm.gitopsConfig['folderStructureStrategy'] = 'ENV_PER_APP'
        helmHelm.gitopsConfig['deployments']['destinationRootPath'] = 'apps'

        helmHelm.preValidation('staging')

        assertThat(dockerMock.actualImages[0]).contains('helmImage')
        assertThat(scriptMock.actualShArgs[0]).isEqualTo('helm repo add chartRepo repoUrl')
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('helm repo update')
        assertThat(scriptMock.actualShArgs[2]).isEqualTo('helm pull chartRepo/chartName --version=1.0 --untar --untardir=workspace/.helmChartTempDir/chart')
        assertThat(scriptMock.actualShArgs[3]).isEqualTo('[returnStdout:true, script:helm values workspace/.helmChartTempDir/chart/chartName -f workspace/k8s/values-staging.yaml -f workspace/k8s/values-shared.yaml ]')
        assertThat(scriptMock.actualWriteFileArgs[0]).isEqualTo('[file:workspace/.helmChartTempDir/mergedValues.yaml, text:[helm repo add chartRepo repoUrl, helm repo update, helm pull chartRepo/chartName --version=1.0 --untar --untardir=workspace/.helmChartTempDir/chart, [returnStdout:true, script:helm values workspace/.helmChartTempDir/chart/chartName -f workspace/k8s/values-staging.yaml -f workspace/k8s/values-shared.yaml ]]]')
        assertThat(scriptMock.actualWriteFileArgs[1]).isEqualTo('''[file:apps/app/staging/applicationRelease.yaml, text:apiVersion: helm.fluxcd.io/v1
kind: HelmRelease
metadata:
  name: app
  namespace: fluxv1-staging
  annotations:
    fluxcd.io/automated: "false"
spec:
  releaseName: app
  chart:
    repository: repoUrl
    name: chartName
    version: 1.0
  values:
    ---
    #this part is only for PlainTest regarding updating the image name
    spec:
      template:
        spec:
          containers:
            - name: \'application\'
              image: \'oldImageName\'
    #this part is only for HelmTest regarding changing the yaml values
    to:
      be:
        changed: \'oldValue\'
]''')
    }

    @Test
    void 'values files getting parameters attached with gitRepo'() {
        def output = helmGit.valuesFilesWithParameter(['file1.yaml', 'file2.yaml'] as String[])
        assertThat(output).isEqualTo('-f file1.yaml -f file2.yaml ')
    }

    @Test
    void 'values files getting parameters attached with helmRepo'() {
        def output = helmHelm.valuesFilesWithParameter(['file1.yaml', 'file2.yaml'] as String[])
        assertThat(output).isEqualTo('-f file1.yaml -f file2.yaml ')
    }

    @Test
    void 'flux helm validates with yamllint and kubeval and helmKubeval'() {
        helmHelm.validate('staging')

        assertThat(scriptMock.actualEchoArgs[0]).isEqualTo('Starting validator Yamllint for FLUX in PLAIN resources')
        assertThat(scriptMock.actualEchoArgs[1]).isEqualTo('Skipping validator Yamllint because it is configured as enabled=false or doesn\'t support the given gitopsTool=FLUX or sourceType=HELM')
        assertThat(scriptMock.actualEchoArgs[2]).isEqualTo('Starting validator Kubeval for FLUX in PLAIN resources')
        assertThat(scriptMock.actualEchoArgs[3]).isEqualTo('Skipping validator Kubeval because it is configured as enabled=false or doesn\'t support the given gitopsTool=FLUX or sourceType=HELM')
        assertThat(scriptMock.actualEchoArgs[4]).isEqualTo('Skipping validator HelmKubeval because it is configured as enabled=false or doesn\'t support the given gitopsTool=FLUX or sourceType=PLAIN')
        assertThat(scriptMock.actualEchoArgs[5]).isEqualTo('Starting validator HelmKubeval for FLUX in HELM resources')

        assertThat(scriptMock.actualShArgs[0]).isEqualTo('yamllint -f standard staging/app')
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('kubeval -d staging/app -v null --strict --ignore-missing-schemas')
        assertThat(scriptMock.actualShArgs[2]).isEqualTo('helm kubeval workspace/.helmChartTempDir/chart/chartName -f workspace/.helmChartTempDir/mergedValues.yaml -v null --strict --ignore-missing-schemas')
    }

    @Test
    void 'argo helm validates with yamllint and kubeval'() {
        helmHelm.gitopsConfig['gitopsTool'] = 'ARGO'
        helmHelm.validate('staging')

        assertThat(scriptMock.actualEchoArgs[0]).isEqualTo('Starting validator Yamllint for ARGO in PLAIN resources')
        assertThat(scriptMock.actualEchoArgs[1]).isEqualTo('Skipping validator Yamllint because it is configured as enabled=false or doesn\'t support the given gitopsTool=ARGO or sourceType=HELM')
        assertThat(scriptMock.actualEchoArgs[2]).isEqualTo('Starting validator Kubeval for ARGO in PLAIN resources')
        assertThat(scriptMock.actualEchoArgs[3]).isEqualTo('Skipping validator Kubeval because it is configured as enabled=false or doesn\'t support the given gitopsTool=ARGO or sourceType=HELM')
        assertThat(scriptMock.actualEchoArgs[4]).isEqualTo('Skipping validator HelmKubeval because it is configured as enabled=false or doesn\'t support the given gitopsTool=ARGO or sourceType=PLAIN')
        assertThat(scriptMock.actualEchoArgs[5]).isEqualTo('Skipping validator HelmKubeval because it is configured as enabled=false or doesn\'t support the given gitopsTool=ARGO or sourceType=HELM')

        assertThat(scriptMock.actualShArgs[0]).isEqualTo('yamllint -f standard staging/app')
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('kubeval -d staging/app -v null --strict --ignore-missing-schemas')
    }

    @Test
    void 'creating helm release with local repo'() {
        helmLocal.preValidation('staging')

        assertThat(dockerMock.actualImages[0]).contains('helmImage')
        assertThat(scriptMock.actualShArgs[0]).isEqualTo('[returnStdout:true, script:helm values workspace/chart/path -f workspace/k8s/values-staging.yaml -f workspace/k8s/values-shared.yaml ]')
    }
}
