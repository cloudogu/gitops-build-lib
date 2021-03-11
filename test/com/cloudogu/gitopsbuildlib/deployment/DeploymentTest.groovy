package com.cloudogu.gitopsbuildlib.deployment

import com.cloudogu.gitopsbuildlib.ScriptMock
import com.cloudogu.gitopsbuildlib.validation.HelmKubeval
import com.cloudogu.gitopsbuildlib.validation.Kubeval
import com.cloudogu.gitopsbuildlib.validation.Yamllint
import org.junit.Test
import static org.assertj.core.api.Assertions.assertThat

class DeploymentTest {

    def scriptMock = new ScriptMock()

    Deployment deploymentUnderTest = new DeploymentUnderTest(scriptMock.mock, [
        application: 'app',
        stages: [
            staging: [:]
        ],
        deployments: [
            sourcePath: 'k8s'
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
        assertThat(scriptMock.actualEchoArgs[0]).isEqualTo('Copying k8s payload from application repo to gitOps Repo: \'k8s/staging/*\' to \'staging/app/k8s\'')
        assertThat(scriptMock.actualShArgs[0]).isEqualTo('mkdir -p staging/app/k8s/')
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('mkdir -p .config/')
        assertThat(scriptMock.actualShArgs[2]).isEqualTo('cp -r workspace/k8s/staging/* staging/app/k8s/ || true')
        assertThat(scriptMock.actualShArgs[3]).isEqualTo('cp workspace/*.yamllint.yaml .config/ || true')
    }

    @Test
    void 'validate three times for every validator'() {
        deploymentUnderTest.validate('staging')
        assertThat(scriptMock.actualEchoArgs[0]).isEqualTo('Executing validator yamllint')
        assertThat(scriptMock.actualEchoArgs[1]).isEqualTo('Executing validator kubeval')
        assertThat(scriptMock.actualEchoArgs[2]).isEqualTo('Executing validator helmKubeval')
    }

    @Test
    void 'create configmaps from files with no namespace set'() {

        deploymentUnderTest.createFileConfigmaps('staging')

        assertThat(scriptMock.actualShArgs[0]).isEqualTo('[returnStdout:true, script:KUBECONFIG=pwd/.kube/config kubectl create configmap index --from-file=index.html=workspace/k8s/../index.html --dry-run=client -o yaml -n staging]')

        assertThat(scriptMock.actualWriteFileArgs[0]).isEqualTo('[file:pwd/.kube/config, text:apiVersion: v1\n' +
            'clusters:\n' +
            '- cluster:\n' +
            '    certificate-authority-data: DATA+OMITTED\n' +
            '    server: https://localhost\n' +
            '  name: self-hosted-cluster\n' +
            'contexts:\n' +
            '- context:\n' +
            '    cluster: self-hosted-cluster\n' +
            '    user: svcs-acct-dply\n' +
            '  name: svcs-acct-context\n' +
            'current-context: svcs-acct-context\n' +
            'kind: Config\n' +
            'preferences: {}\n' +
            'users:\n' +
            '- name: svcs-acct-dply\n' +
            '  user:\n' +
            '    token: DATA+OMITTED]')
        assertThat(scriptMock.actualWriteFileArgs[1]).contains('[file:staging/app/generatedResources/index.yaml')
    }

    @Test
    void 'create configmaps from files with namespace set'() {

        deploymentUnderTest.gitopsConfig.stages.staging = [ namespace: 'fluxv1-staging' ]

        deploymentUnderTest.createFileConfigmaps('staging')

        assertThat(scriptMock.actualShArgs[0]).isEqualTo('[returnStdout:true, script:KUBECONFIG=pwd/.kube/config kubectl create configmap index --from-file=index.html=workspace/k8s/../index.html --dry-run=client -o yaml -n fluxv1-staging]')

        assertThat(scriptMock.actualWriteFileArgs[0]).isEqualTo('[file:pwd/.kube/config, text:apiVersion: v1\n' +
            'clusters:\n' +
            '- cluster:\n' +
            '    certificate-authority-data: DATA+OMITTED\n' +
            '    server: https://localhost\n' +
            '  name: self-hosted-cluster\n' +
            'contexts:\n' +
            '- context:\n' +
            '    cluster: self-hosted-cluster\n' +
            '    user: svcs-acct-dply\n' +
            '  name: svcs-acct-context\n' +
            'current-context: svcs-acct-context\n' +
            'kind: Config\n' +
            'preferences: {}\n' +
            'users:\n' +
            '- name: svcs-acct-dply\n' +
            '  user:\n' +
            '    token: DATA+OMITTED]')
        assertThat(scriptMock.actualWriteFileArgs[1]).contains('[file:staging/app/generatedResources/index.yaml')
    }

    class DeploymentUnderTest extends Deployment {

        DeploymentUnderTest(Object script, Object gitopsConfig) {
            super(script, gitopsConfig)
        }

        @Override
        def createPreValidation(String stage) {
            return null
        }

        @Override
        def createPostValidation(String stage) {
            return null
        }
    }
}
