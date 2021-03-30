package com.cloudogu.gitopsbuildlib

import com.cloudogu.ces.cesbuildlib.DockerMock
import com.cloudogu.ces.cesbuildlib.Git
import com.cloudogu.ces.cesbuildlib.SCMManager

import com.cloudogu.gitopsbuildlib.validation.Kubeval
import com.cloudogu.gitopsbuildlib.validation.Yamllint
import com.lesfurets.jenkins.unit.BasePipelineTest
import groovy.mock.interceptor.StubFor
import groovy.yaml.YamlSlurper
import org.junit.jupiter.api.*
import org.mockito.ArgumentCaptor

import static com.lesfurets.jenkins.unit.MethodCall.callArgsToString
import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeployViaGitopsTest extends BasePipelineTest {

    class CesBuildLibMock {
        def Git = [:]
        def Docker = [:]
        def SCMManager = [:]
    }

    def git
    def docker
    def scmm

    def gitRepo

    def cesBuildLibMock
    def deployViaGitops

    static final String EXPECTED_APPLICATION = 'app'

    String helmImage = 'ghcr.io/cloudogu/helm:3.4.1-1'

    Map gitopsConfig(Map stages, Map deployments) {
        return [
            scmmCredentialsId       : 'scmManagerCredentials',
            scmmConfigRepoUrl       : 'configRepositoryUrl',
            scmmPullRequestBaseUrl  : 'http://scmm-scm-manager/scm',
            scmmPullRequestRepo     : 'fluxv1/gitops',
            cesBuildLibRepo         : 'cesBuildLibRepo',
            cesBuildLibVersion      : 'cesBuildLibVersion',
            cesBuildLibCredentialsId: 'cesBuildLibCredentialsId',
            application             : 'application',
            mainBranch              : 'main',
            deployments             : deployments,
            validators              : [
                kubeval : [
                    validator: new Kubeval(deployViaGitops),
                    enabled  : true,
                    config   : [
                        // We use the helm image (that also contains kubeval plugin) to speed up builds by allowing to reuse image
                        image           : helmImage,
                        k8sSchemaVersion: '1.18.1'
                    ]
                ],
                yamllint: [
                    validator: new Yamllint(deployViaGitops),
                    enabled  : true,
                    config   : [
                        image  : 'cytopia/yamllint:1.25-0.7',
                        // Default to relaxed profile because it's feasible for mere mortalYAML programmers.
                        // It still fails on syntax errors.
                        profile: 'relaxed'
                    ]
                ]
            ],
            stages                  : stages
        ]
    }

    def plainDeployment = [
        sourcePath: 'k8s',
        plain     : [
            updateImages: [
                [deploymentFilename: "deployment.yaml",
                 containerName     : 'application',
                 imageName         : 'newImageName']
            ]
        ]
    ]

    def singleStages = [
        staging: [deployDirectly: true]
    ]

    def multipleStages = [
        staging   : [deployDirectly: true],
        production: [deployDirectly: false],
        qa        : [deployDirectly: false]
    ]

    @BeforeAll
    void setUp() throws Exception {
        scriptRoots += 'vars'
        super.setUp()
    }

    @BeforeEach
    void init() {
        deployViaGitops = loadScript('vars/deployViaGitops.groovy')
        binding.getVariable('currentBuild').result = 'SUCCESS'
        setupGlobals(deployViaGitops)

        cesBuildLibMock = new CesBuildLibMock()
        git = mock(Git.class)
        docker = new DockerMock().createMock()
        scmm = mock(SCMManager.class)

        cesBuildLibMock.Docker.new = {
            return docker
        }

        cesBuildLibMock.Git.new = { def script, String credentials ->
            return git
        }

        cesBuildLibMock.SCMManager.new = { def script, String prBaseUrl, String scmmCredentialsId ->
            return scmm
        }

        def configYaml = '''\
---
spec:
  template:
    spec:
      containers:
        - name: 'application'
          image: 'oldImageName'
'''

        gitRepo = new StubFor(GitRepo)
        gitRepo.demand.with {
            create { new GitRepo('test', 'test', 'test', 'test', 'test') }
            // this needs to be defined three times for our three stages.
            // the authorName changes are only for verifying purposes within this testcase.
            // in normal usage the authorName is the same
            // for staging
            getCommitMessage { '#0001' }
            getRepositoryUrl { 'backend/k8s-gitops/' }
            getAuthorName { 'staging' }
            getAuthorEmail { 'authorName@email.de' }
            getCommitHash { '1234abcd' }

            // for production
            getCommitMessage { '#0001' }
            getRepositoryUrl { 'backend/k8s-gitops/' }
            getAuthorName { 'production' }
            getAuthorEmail { 'authorName@email.de' }
            getCommitHash { '1234abcd' }

            // for qa
            getCommitMessage { '#0001' }
            getRepositoryUrl { 'backend/k8s-gitops/' }
            getAuthorName { 'qa' }
            getAuthorEmail { 'authorName@email.de' }
            getCommitHash { '1234abcd' }
        }

        deployViaGitops.metaClass.initCesBuildLib = { String repo, String version, String credentialsId ->
            return cesBuildLibMock
        }

        deployViaGitops.metaClass.pwd = {
            return '/'
        }

        deployViaGitops.metaClass.readYaml = {
            return new YamlSlurper().parseText(configYaml)
        }

        deployViaGitops.metaClass.writeYaml = { LinkedHashMap args ->
            echo "filepath is: ${args.file}, data is: ${args.data}, overwrite is: ${args.overwrite}"
        }

        when(git.commitHashShort).thenReturn('1234abcd')
    }

    @AfterEach
    void tearDown() throws Exception {
        // always reset metaClass after messing with it to prevent changes from leaking to other tests
        deployViaGitops.metaClass = null
    }

    @Test
    void 'default values are set'() {

        deployViaGitops.metaClass.deploy = { Map actualGitOpsConfig ->
            assertGitOpsConfigWithoutInstances(actualGitOpsConfig, deployViaGitops.getDefaultConfig())
        }

        deployViaGitops([:])
    }

    @Test
    void 'default values can be overwritten'() {

        deployViaGitops.metaClass.deploy = { Map actualGitOpsConfig ->
            assertThat(actualGitOpsConfig.cesBuildLibRepo).isEqualTo('abc')
            assertThat(actualGitOpsConfig.cesBuildLibCredentialsId).isEqualTo('testuser')
        }

        deployViaGitops([cesBuildLibRepo: 'abc', cesBuildLibCredentialsId: 'testuser'])
    }

    @Test
    void 'default stages defined as staging and production'() {
        deployViaGitops.metaClass.deploy = { Map actualGitOpsConfig ->
            assertThat(actualGitOpsConfig.stages.containsKey('staging')).isEqualTo(true)
            assertThat(actualGitOpsConfig.stages.containsKey('production')).isEqualTo(true)
            assertThat(actualGitOpsConfig.stages.staging.deployDirectly).isEqualTo(true)
            assertThat(actualGitOpsConfig.stages.production.deployDirectly).isEqualTo(false)
        }

        deployViaGitops([:])
    }

    @Test
    void 'stages definition gets overwritten rather than merged'() {
        deployViaGitops.metaClass.deploy = { Map actualGitOpsConfig ->
            assertThat(actualGitOpsConfig.stages.containsKey('staging')).isEqualTo(true)
            assertThat(actualGitOpsConfig.stages.containsKey('production')).isEqualTo(false)
            assertThat(actualGitOpsConfig.stages.staging.deployDirectly).isEqualTo(true)
        }

        deployViaGitops(gitopsConfig(singleStages, plainDeployment))
    }

    @Test
    void 'default validator can be disabled'() {

        deployViaGitops.metaClass.deploy = { Map actualGitOpsConfig ->
            assertThat(actualGitOpsConfig.validators.kubeval.enabled).isEqualTo(false)
            assertThat(actualGitOpsConfig.validators.kubeval.validator).isNotNull()
            assertThat(actualGitOpsConfig.validators.yamllint.enabled).isEqualTo(true)
        }

        deployViaGitops([
            validators: [
                kubeval: [
                    enabled: false
                ]
            ]
        ])
    }

    @Test
    void 'custom validator can be added'() {

        deployViaGitops.metaClass.deploy = { Map actualGitOpsConfig ->
            assertThat(actualGitOpsConfig.validators.myVali.config.a).isEqualTo('b')
            assertThat(actualGitOpsConfig.validators.yamllint.enabled).isEqualTo(true)
            assertThat(actualGitOpsConfig.validators.yamllint.enabled).isEqualTo(true)
        }

        deployViaGitops([
            validators: [
                myVali: [
                    validator: {},
                    enabled  : true,
                    config   : [
                        a: 'b'
                    ]
                ]
            ]
        ])
    }

    @Test
    void 'single stage deployment via gitops'() {

        when(git.areChangesStagedForCommit()).thenReturn(true)

        gitRepo.use {
            deployViaGitops.call(gitopsConfig(singleStages, plainDeployment))
        }

        println(gitopsConfig(singleStages, plainDeployment))

        // testing deploy
        assertThat(helper.callStack.findAll { call -> call.methodName == "dir" }.any { call ->
            callArgsToString(call).contains(".configRepoTempDir")
        }).isTrue()

        ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class)
        verify(git).call(argumentCaptor.capture())
        assertThat(argumentCaptor.getValue().url).isEqualTo('configRepositoryUrl')
        assertThat(argumentCaptor.getValue().branch).isEqualTo('main')
        verify(git, times(1)).fetch()

        assertThat(helper.callStack.findAll { call -> call.methodName == "sh" }.any { call ->
            callArgsToString(call).equals("rm -rf .configRepoTempDir")
        }).isTrue()

        // testing syncGitopsRepoPerStage
        verify(git, times(1)).checkoutOrCreate('main')

        // testing commitAndPushToStage
        verify(git, times(1)).add('.')

        ArgumentCaptor<String> argumentCaptor2 = ArgumentCaptor.forClass(String.class)
        verify(git).commit(argumentCaptor2.capture(), anyString(), anyString())
        assertThat(argumentCaptor2.getValue()).isEqualTo('[staging] #0001 backend/k8s-gitops@1234abcd')

        argumentCaptor2 = ArgumentCaptor.forClass(String.class)
        verify(git).pushAndPullOnFailure(argumentCaptor2.capture())
        assertThat(argumentCaptor2.getValue()).isEqualTo('origin main')


        assertThat(deployViaGitops.currentBuild.description).isEqualTo('GitOps commits: staging (1234abcd)\nImage: [newImageName]')
    }

    @Test
    void 'multi-stage deployment via GitOps'() {
        when(git.areChangesStagedForCommit()).thenReturn(true)

        gitRepo.use {
            deployViaGitops.call(gitopsConfig(multipleStages, plainDeployment))
        }

        // testing deploy
        assertThat(helper.callStack.findAll { call -> call.methodName == "dir" }.any { call ->
            callArgsToString(call).contains(".configRepoTempDir")
        }).isTrue()

        ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class)
        verify(git).call(argumentCaptor.capture())
        assertThat(argumentCaptor.getValue().url).isEqualTo('configRepositoryUrl')
        assertThat(argumentCaptor.getValue().branch).isEqualTo('main')
        verify(git, times(1)).fetch()

        assertThat(helper.callStack.findAll { call -> call.methodName == "sh" }.any { call ->
            callArgsToString(call).equals("rm -rf .configRepoTempDir")
        }).isTrue()

        // testing syncGitopsRepoPerStage
        verify(git, times(3)).checkoutOrCreate('main')
        verify(git, times(1)).checkoutOrCreate('production_application')
        verify(git, times(1)).checkoutOrCreate('qa_application')


        // testing commitAndPushToStage
        verify(git, times(3)).add('.')

        ArgumentCaptor<String> argumentCaptor2 = ArgumentCaptor.forClass(String.class)
        verify(git).commit(argumentCaptor2.capture(), eq('staging'), anyString())
        println("before argument get value")
        assertThat(argumentCaptor2.getValue()).isEqualTo('[staging] #0001 backend/k8s-gitops@1234abcd')
        println("after argument get value")

        argumentCaptor2 = ArgumentCaptor.forClass(String.class)
        verify(git).commit(argumentCaptor2.capture(), eq('production'), anyString())
        assertThat(argumentCaptor2.getValue()).isEqualTo('[production] #0001 backend/k8s-gitops@1234abcd')

        argumentCaptor2 = ArgumentCaptor.forClass(String.class)
        verify(git).commit(argumentCaptor2.capture(), eq('qa'), anyString())
        assertThat(argumentCaptor2.getValue()).isEqualTo('[qa] #0001 backend/k8s-gitops@1234abcd')

        verify(git, times(3)).pushAndPullOnFailure(anyString())

        assertThat(deployViaGitops.currentBuild.description).isEqualTo('GitOps commits: staging (1234abcd); production (1234abcd); qa (1234abcd)\nImage: [newImageName]')
    }

    @Test
    void 'no changes in git with multiple stages'() {
        when(git.areChangesStagedForCommit()).thenReturn(false)

        gitRepo.use {
            deployViaGitops.call(gitopsConfig(multipleStages, plainDeployment))
        }

        List<String> stringArgs = []
        helper.callStack.findAll { call -> call.methodName == "echo" }.any { call ->
            stringArgs += callArgsToString(call)
        }
        assertThat(stringArgs.contains('No changes on gitOps repo for staging (branch: main)'))
        assertThat(stringArgs.contains('No changes on gitOps repo for staging (branch: production_application)'))
        assertThat(stringArgs.contains('No changes on gitOps repo for staging (branch: qa_application)'))
    }

    @Test
    void 'no changes in git with single stages'() {
        when(git.areChangesStagedForCommit()).thenReturn(false)

        gitRepo.use {
            deployViaGitops.call(gitopsConfig(singleStages, plainDeployment))
        }

        assertThat(
            helper.callStack.findAll { call -> call.methodName == "echo" }.any { call ->
                callArgsToString(call).equals("No changes on gitOps repo for staging (branch: main). Not committing or pushing.")
            }).isTrue()
    }

    @Test
    void 'preparing gitRepo returns config'() {

        def stub = new StubFor(GitRepo)

        stub.demand.with {
            create { new GitRepo('test', 'test', 'test', 'test', 'test') }
        }

        stub.use {
            def output = deployViaGitops.prepareGitRepo(git)
            assert output.applicationRepo != null
            assert output.configRepoTempDir == '.configRepoTempDir'
        }
    }

    @Test
    void 'returns correct build description'() {
        def output = deployViaGitops.createBuildDescription('changes', 'imageName')
        assert output == 'GitOps commits: changes\nImage: imageName'
    }

    @Test
    void 'returns correct build description without imageName'() {
        def output = deployViaGitops.createBuildDescription('changes')
        assert output == 'GitOps commits: changes'
    }

    @Test
    void 'return No Changes if no changes are present'() {
        def output = deployViaGitops.createBuildDescription('', 'imageName')
        assert output == 'GitOps commits: No changes\nImage: imageName'
    }

    @Test
    void 'return No Changes if no changes are present without imageName'() {
        def output = deployViaGitops.createBuildDescription('')
        assert output == 'GitOps commits: No changes'
    }

    @Test
    void 'changes are being aggregated'() {
        def changes = ['1', '2', '3']
        def output = deployViaGitops.aggregateChangesOnGitOpsRepo(changes)
        assert output == '1; 2; 3'
    }

    @Test
    void 'error on single missing mandatory field'() {

        def gitopsConfigMissingMandatoryField = [
            scmmConfigRepoUrl     : 'configRepositoryUrl',
            scmmPullRequestBaseUrl: 'configRepositoryPRBaseUrl',
            scmmPullRequestRepo   : 'scmmPullRequestRepo',
            application           : 'application',
            deployments           : [
                sourcePath: 'k8s',
                plain     : [
                    updateImages: [
                        [filename     : "deployment.yaml",
                         containerName: 'application',
                         imageName    : 'imageName']
                    ]
                ]
            ],
            stages                : [
                staging   : [deployDirectly: true],
                production: [deployDirectly: false],
                qa        : []
            ]
        ]

        gitRepo.use {
            deployViaGitops.call(gitopsConfigMissingMandatoryField)
        }

        assertThat(
            helper.callStack.findAll { call -> call.methodName == "error" }.any { call ->
                callArgsToString(call).contains("[scmmCredentialsId]")
            }).isTrue()
    }

    @Test
    void 'error on single non valid mandatory field'() {

        def gitopsConfigMissingMandatoryField = [
            scmmCredentialsId     : 'scmManagerCredentials',
            scmmConfigRepoUrl     : 'configRepositoryUrl',
            scmmPullRequestBaseUrl: '',
            scmmPullRequestRepo   : 'scmmPullRequestRepo',
            scmmPullRequestUrl    : 'configRepositoryPRUrl',
            application           : 'application',
            deployments           : [
                sourcePath: 'k8s',
                plain     : [
                    updateImages: [
                        [filename     : "deployment.yaml",
                         containerName: 'application',
                         imageName    : 'imageName']
                    ]
                ]
            ],
            stages                : [
                staging   : [deployDirectly: true],
                production: [deployDirectly: false],
                qa        : []
            ]
        ]

        gitRepo.use {
            deployViaGitops.call(gitopsConfigMissingMandatoryField)
        }

        assertThat(
            helper.callStack.findAll { call -> call.methodName == "error" }.any { call ->
                callArgsToString(call).contains("[scmmPullRequestBaseUrl]")
            }).isTrue()
    }

    @Test
    void 'error on missing or non valid values on mandatory fields'() {

        def gitopsConfigMissingMandatoryField = [
            scmmPullRequestBaseUrl: null,
            application           : '',
            stages                : []
        ]

        gitRepo.use {
            deployViaGitops.call(gitopsConfigMissingMandatoryField)
        }

        assertThat(
            helper.callStack.findAll { call -> call.methodName == "error" }.any { call ->
                callArgsToString(call).contains("[scmmCredentialsId, scmmConfigRepoUrl, scmmPullRequestBaseUrl, scmmPullRequestRepo, application, stages]")
            }).isTrue()
    }

    private static void setupGlobals(Script script) {
        script.metaClass.getAppDockerRegistry = { EXPECTED_REGISTRY }
        script.application = EXPECTED_APPLICATION
        script.repoDigest = null
        script.forceDeployStaging = null
        script.reDeployImageVersion = null
        script.skipQualityAssurance = null
        script.configRepositoryPRUrl = "scm/repo"
    }

    void assertGitOpsConfigWithoutInstances(Map actualGitOpsConfig, Map expected) {
        // Remove Instance IDs, e.g. Yamllint@1234567 because they are generate on each getDefaultConfig() call.
        assertThat(actualGitOpsConfig.toString().replaceAll('@.*,', ','))
            .isEqualTo(deployViaGitops.getDefaultConfig().toString().replaceAll('@.*,', ','))
    }
}
