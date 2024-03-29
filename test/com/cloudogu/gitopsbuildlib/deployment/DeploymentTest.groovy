package com.cloudogu.gitopsbuildlib.deployment

import com.cloudogu.gitopsbuildlib.ScriptMock
import com.cloudogu.gitopsbuildlib.validation.HelmKubeval
import com.cloudogu.gitopsbuildlib.validation.Kubeval
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
            destinationRootPath: '.',
            plain: [:]
        ],
        folderStructureStrategy: 'GLOBAL_ENV',
        buildImages: [
            kubectl: [
                image: "http://my-private-registry.com/repo/kubectlImage",
                credentialsId: "credentials"
            ]
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
    void 'creating folders for plain deployment with ENV_PER_APP and other destinationRootPath'() {
        deploymentUnderTest.gitopsConfig['folderStructureStrategy'] = 'ENV_PER_APP'
        deploymentUnderTest.gitopsConfig['deployments']['destinationRootPath'] = 'apps'

        deploymentUnderTest.createFoldersAndCopyK8sResources('staging',)

        assertThat(scriptMock.actualEchoArgs[0]).isEqualTo('Copying k8s payload from application repo to gitOps Repo: \'k8s/staging/*\' to \'apps/app/staging/\'')
        assertThat(scriptMock.actualShArgs[0]).isEqualTo('mkdir -p apps/app/staging/')
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('mkdir -p .config/')
        assertThat(scriptMock.actualShArgs[2]).isEqualTo('cp -r workspace/k8s/staging/* apps/app/staging/ || true')
        assertThat(scriptMock.actualShArgs[3]).isEqualTo('cp workspace/*.yamllint.yaml .config/ || true')
    }
    
    @Test
    void 'create configmaps from files'() {

        deploymentUnderTest.createFileConfigmaps('staging')

        assertThat(scriptMock.dockerMock.actualRegistryArgs[0]).isEqualTo('https://http://my-private-registry.com/repo')
        assertThat(scriptMock.dockerMock.actualRegistryArgs[1]).isEqualTo('credentials')

        assertThat(scriptMock.actualShArgs[0]).isEqualTo('[returnStdout:true, script:kubectl create configmap index --from-file=index.html=workspace/k8s/../index.html --dry-run=client -o yaml -n fluxv1-staging]')

        assertThat(scriptMock.actualWriteFileArgs[0]).contains('[file:staging/app/generatedResources/index.yaml')
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

        @Override
        def validate(String stage) {
            return null
        }
    }
}
