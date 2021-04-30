package com.cloudogu.gitopsbuildlib.deployment

import com.cloudogu.gitopsbuildlib.ScriptMock
import com.cloudogu.gitopsbuildlib.validation.GitopsTool
import com.cloudogu.gitopsbuildlib.validation.HelmKubeval
import com.cloudogu.gitopsbuildlib.validation.Kubeval
import com.cloudogu.gitopsbuildlib.validation.SourceType
import com.cloudogu.gitopsbuildlib.validation.Validator
import com.cloudogu.gitopsbuildlib.validation.Yamllint
import org.junit.jupiter.api.*
import static org.assertj.core.api.Assertions.assertThat

class DeploymentTest {

    def scriptMock = new ScriptMock()

    Deployment deploymentUnderTest = new DeploymentUnderTest(scriptMock.mock, [
        application: 'app',
        gitopsTool: 'FLUX',
        stages: [
            staging: [
                namespace: 'fluxv1-staging'
            ]
        ],
        deployments: [
            sourcePath: 'k8s',
            plain: [:]
        ],
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
    ] as Map)

    @Test
    void 'creating folders for plain deployment'() {
        deploymentUnderTest.createFoldersAndCopyK8sResources('staging',)
        assertThat(scriptMock.actualEchoArgs[0]).isEqualTo('Copying k8s payload from application repo to gitOps Repo: \'k8s/staging/*\' to \'staging/app/\'')
        assertThat(scriptMock.actualShArgs[0]).isEqualTo('mkdir -p staging/app/')
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('mkdir -p .config/')
        assertThat(scriptMock.actualShArgs[2]).isEqualTo('cp -r workspace/k8s/staging/* staging/app/ || true')
        assertThat(scriptMock.actualShArgs[3]).isEqualTo('cp workspace/*.yamllint.yaml .config/ || true')
    }
    
    @Test
    void 'create configmaps from files'() {

        deploymentUnderTest.createFileConfigmaps('staging')

        assertThat(scriptMock.actualShArgs[0]).isEqualTo('[returnStdout:true, script:KUBECONFIG=pwd/.kube/config kubectl create configmap index --from-file=index.html=workspace/k8s/../index.html --dry-run=client -o yaml -n fluxv1-staging]')

        assertThat(scriptMock.actualWriteFileArgs[0]).isEqualTo('''[file:pwd/.kube/config, text:apiVersion: v1
clusters:
- cluster:
    certificate-authority-data: DATA+OMITTED
    server: https://localhost
  name: self-hosted-cluster
contexts:
- context:
    cluster: self-hosted-cluster
    user: svcs-acct-dply
  name: svcs-acct-context
current-context: svcs-acct-context
kind: Config
preferences: {}
users:
- name: svcs-acct-dply
  user:
    token: DATA+OMITTED]''')
        assertThat(scriptMock.actualWriteFileArgs[1]).contains('[file:staging/app/generatedResources/index.yaml')
    }

    @Test
    void 'flux plain validates with yamllint and kubeval'() {
        deploymentUnderTest.validate('staging')

        assertThat(scriptMock.actualEchoArgs[0]).isEqualTo('Starting validator Yamllint for FLUX in PLAIN resources')
        assertThat(scriptMock.actualEchoArgs[1]).isEqualTo('Starting validator Kubeval for FLUX in PLAIN resources')
        assertThat(scriptMock.actualEchoArgs[2]).isEqualTo('Starting validator HelmKubeval for FLUX in HELM resources')

        assertThat(scriptMock.actualShArgs[0]).isEqualTo('yamllint -f standard staging/app')
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('kubeval -d staging/app -v null --strict --ignore-missing-schemas')
        assertThat(scriptMock.actualEchoArgs[3]).isEqualTo('Not executing HelmKubeval because this is not a helm deployment')
    }

    @Test
    void 'flux helm validates with yamllint and kubeval and helmKubeval'() {
        deploymentUnderTest.gitopsConfig['deployments'] = [
            sourcePath: 'k8s',
            helm: [:]
        ]
        deploymentUnderTest.validate('staging')

        assertThat(scriptMock.actualEchoArgs[0]).isEqualTo('Starting validator Yamllint for FLUX in PLAIN resources')
        assertThat(scriptMock.actualEchoArgs[1]).isEqualTo('Starting validator Kubeval for FLUX in PLAIN resources')
        assertThat(scriptMock.actualEchoArgs[2]).isEqualTo('Starting validator HelmKubeval for FLUX in HELM resources')

        assertThat(scriptMock.actualShArgs[0]).isEqualTo('yamllint -f standard staging/app')
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('kubeval -d staging/app -v null --strict --ignore-missing-schemas')
        assertThat(scriptMock.actualShArgs[2]).isEqualTo('helm kubeval workspace/.helmChartTempDir/chart/ -f workspace/.helmChartTempDir/mergedValues.yaml -v null --strict --ignore-missing-schemas')
    }

    @Test
    void 'argo plain validates with yamllint and kubeval'() {
        deploymentUnderTest.gitopsConfig['gitopsTool'] = 'ARGO'
        deploymentUnderTest.validate('staging')

        assertThat(scriptMock.actualEchoArgs[0]).isEqualTo('Starting validator Yamllint for ARGO in PLAIN resources')
        assertThat(scriptMock.actualEchoArgs[1]).isEqualTo('Starting validator Kubeval for ARGO in PLAIN resources')

        assertThat(scriptMock.actualShArgs[0]).isEqualTo('yamllint -f standard staging/app')
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('kubeval -d staging/app -v null --strict --ignore-missing-schemas')
    }

    @Test
    void 'argo helm validates with yamllint and kubeval'() {
        deploymentUnderTest.gitopsConfig['deployments'] = [
            sourcePath: 'k8s',
            helm: [:]
        ]
        deploymentUnderTest.gitopsConfig['gitopsTool'] = 'ARGO'
        deploymentUnderTest.validate('staging')

        assertThat(scriptMock.actualEchoArgs[0]).isEqualTo('Starting validator Yamllint for ARGO in PLAIN resources')
        assertThat(scriptMock.actualEchoArgs[1]).isEqualTo('Starting validator Kubeval for ARGO in PLAIN resources')

        assertThat(scriptMock.actualShArgs[0]).isEqualTo('yamllint -f standard staging/app')
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('kubeval -d staging/app -v null --strict --ignore-missing-schemas')

    }

    class DeploymentUnderTest extends Deployment {

        DeploymentUnderTest(Object script, Object gitopsConfig) {
            super(script, gitopsConfig)
        }

        @Override
        def preValidation(String stage) {
            return null
        }

        @Override
        def postValidation(String stage) {
            return null
        }
    }
}
