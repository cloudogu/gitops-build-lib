import com.cloudogu.ces.cesbuildlib.Docker
import com.cloudogu.ces.cesbuildlib.Git
import com.cloudogu.gitopsbuildlib.GitRepo
import com.lesfurets.jenkins.unit.BasePipelineTest
import groovy.mock.interceptor.StubFor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import groovy.yaml.YamlSlurper
import org.mockito.ArgumentCaptor
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import static org.assertj.core.api.Assertions.assertThat
import static com.lesfurets.jenkins.unit.MethodCall.callArgsToString
import static org.mockito.Mockito.times
import static org.mockito.Mockito.when
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeployViaGitopsTest extends BasePipelineTest {

    class CesBuildLibMock {
        def Git = [:]
        def Docker = [:]
    }

    //TODO naming mock?
    def git
    def docker

    def gitRepo

    def cesBuildLibMock
    def deployViaGitops
    def actualShStringArgs = []

    static final String EXPECTED_OUTPUT = "test"
    static final String EXPECTED_APPLICATION = 'app'
    static final Map GIT_REPO = [
            applicationRepo  : 'applicationRepo',
            configRepoTempDir: 'configRepoTempDir'
    ]
    Map gitopsConfig(Map stages) {
        return [
                scmmCredentialsId : 'scmManagerCredentials',
                scmmConfigRepoUrl : 'configRepositoryUrl',
                scmmPullRequestUrl: 'configRepositoryPRUrl',
                cesBuildLibRepo   : 'cesBuildLibRepo',
                cesBuildLibVersion: 'cesBuildLibVersion',
                application       : 'application',
                mainBranch        : 'main',
                updateImages      : [
                        [deploymentFilename: "deployment.yaml",
                         containerName     : 'application',
                         imageName         : 'newImageName']
                ],
                stages            : stages
        ]
    }


    def singleStages = [
            staging   : [deployDirectly: true]
    ]

    def multipleStages = [
            staging   : [deployDirectly: true],
            production: [deployDirectly: false],
            qa        : []
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
        // TODO re-use DockerMock from class
        docker = mock(Docker.class)
        Docker.Image imageMock = mock(Docker.Image.class)
        when(docker.image(anyString())).thenReturn(imageMock)
        when(imageMock.inside(anyString(), any())).thenAnswer(new Answer<Object>() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                Closure closure = invocation.getArgument(1)
                closure.call()
            }
        })

        cesBuildLibMock.Docker.new = {
            return docker
        }

        cesBuildLibMock.Git.new = { def script, String credentials ->
            return git
        }

        deployViaGitops.metaClass.initCesBuildLib = { String repo, String version ->
            return cesBuildLibMock
        }

        deployViaGitops.metaClass.sh = { String args ->
            actualShStringArgs += args

        }

        deployViaGitops.metaClass.pwd = {
            return '/'
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
            // for staging
            getCommitMessage { 'staging commit message' }
            getRepositoryUrl { 'staging/reponame/' }
            getAuthorName { 'stagingName' }
            getAuthorEmail { 'testerName@email.de' }
            getCommitHash { '1a' }

            // for production
            getCommitMessage { 'production commit message' }
            getRepositoryUrl { 'production/reponame/' }
            getAuthorName { 'productionName' }
            getAuthorEmail { 'testerName@email.de' }
            getCommitHash { '2b' }

            // for qa
            getCommitMessage { 'qa commit message' }
            getRepositoryUrl { 'qa/reponame/' }
            getAuthorName { 'qaName' }
            getAuthorEmail { 'testerName@email.de' }
            getCommitHash { '3c' }
        }

        deployViaGitops.metaClass.readYaml = {
            return new YamlSlurper().parseText(configYaml)
        }

        deployViaGitops.metaClass.writeYaml = { LinkedHashMap args ->
            echo "filepath is: ${args.file}, data is: ${args.data}, overwrite is: ${args.overwrite}"
        }
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
        }

        deployViaGitops([cesBuildLibRepo: 'abc'])
    }

    @Test
    void 'default validator can be disabled'() {

        deployViaGitops.metaClass.deploy = { Map actualGitOpsConfig ->
            assertThat(actualGitOpsConfig.validators.kubeval.enabled).isEqualTo(false)
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
                    validator: { },
                    enabled: true,
                    config: [
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
            deployViaGitops.call(gitopsConfig(singleStages))
        }

        // testing deploy
        assertThat(helper.callStack.findAll { call -> call.methodName == "dir"}.any { call ->
            callArgsToString(call).contains(".configRepoTempDir")
        }).isTrue()

        ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class)
        verify(git).call(argumentCaptor.capture())
        assertThat(argumentCaptor.getValue().url).isEqualTo('configRepositoryUrl')
        assertThat(argumentCaptor.getValue().branch).isEqualTo('main')
        verify(git, times(1)).fetch()

        // testing syncGitopsRepoPerStage
        verify(git, times(1)).checkoutOrCreate('main')

        // TODO somehow this callstack search doesn't find the sh function calls in 'createApplicationFolder'
        // testing createApplicationFolders
//        assertThat(
//            helper.callStack.findAll { call -> call.methodName == "sh"}.any { call ->
//                callArgsToString(call).contains("mkdir -p")
//        }).isTrue()

        // testing validation
        assertThat(
            helper.callStack.findAll { call -> call.methodName == "sh" }.any { call ->
                callArgsToString(call).equals("kubeval -d staging/application/ -v 1.18.1  --strict")
            }).isTrue()
        assertThat(
            helper.callStack.findAll { call -> call.methodName == "sh" }.any { call ->
                callArgsToString(call).equals("yamllint -c .config/config.yamllint.yaml staging/application/")
            }).isTrue()

        //testing updateImageVersion
        assertThat(
            helper.callStack.findAll { call -> call.methodName == "echo" }.any { call ->
                callArgsToString(call).contains("newImageName")
            }).isTrue()

        // testing commitAndPushToStage
        verify(git, times(1)).add('.')

        ArgumentCaptor<String> argumentCaptor2 = ArgumentCaptor.forClass(String.class)
        verify(git).commit(argumentCaptor2.capture(), anyString(), anyString())
        assertThat(argumentCaptor2.getValue()).isEqualTo('[staging] staging/reponame@1a')

        argumentCaptor2 = ArgumentCaptor.forClass(String.class)
        verify(git).pushAndPullOnFailure(argumentCaptor2.capture())
        assertThat(argumentCaptor2.getValue()).isEqualTo('origin main')


        assertThat(deployViaGitops.currentBuild.description).isEqualTo('GitOps commits: staging (1234abcd)\nImage: [newImageName]')
    }

    @Test
    void 'multi-stage deployment via GitOps'() {
        when(git.areChangesStagedForCommit()).thenReturn(true)

        gitRepo.use {
            deployViaGitops.call(gitopsConfig(multipleStages))
        }

        // testing deploy
        assertThat(helper.callStack.findAll { call -> call.methodName == "dir"}.any { call ->
            callArgsToString(call).contains(".configRepoTempDir")
        }).isTrue()

        ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class)
        verify(git).call(argumentCaptor.capture())
        assertThat(argumentCaptor.getValue().url).isEqualTo('configRepositoryUrl')
        assertThat(argumentCaptor.getValue().branch).isEqualTo('main')
        verify(git, times(1)).fetch()

        // testing syncGitopsRepoPerStage
        verify(git, times(3)).checkoutOrCreate('main')
        verify(git, times(1)).checkoutOrCreate('production_application')
        verify(git, times(1)).checkoutOrCreate('qa_application')


        // TODO somehow this callstack search doesn't find the sh function calls in 'createApplicationFolder'
        // testing createApplicationFolders
//        assertThat(
//            helper.callStack.findAll { call -> call.methodName == "sh"}.any { call ->
//                callArgsToString(call).contains("mkdir -p")
//        }).isTrue()

        // testing validation
        assertThat(
            helper.callStack.findAll { call -> call.methodName == "sh" }.any { call ->
                callArgsToString(call).equals("kubeval -d staging/application/ -v 1.18.1  --strict")
            }).isTrue()
        assertThat(
            helper.callStack.findAll { call -> call.methodName == "sh" }.any { call ->
                callArgsToString(call).equals("yamllint -c .config/config.yamllint.yaml staging/application/")
            }).isTrue()

        //testing updateImageVersion
        assertThat(
            helper.callStack.findAll { call -> call.methodName == "echo" }.any { call ->
                callArgsToString(call).contains("newImageName")
            }).isTrue()

        // testing commitAndPushToStage
        verify(git, times(3)).add('.')

        ArgumentCaptor<String> argumentCaptor2 = ArgumentCaptor.forClass(String.class)
        verify(git).commit(argumentCaptor2.capture(), eq('stagingName'), anyString())
        assertThat(argumentCaptor2.getValue()).isEqualTo('[staging] staging/reponame@1a')

        argumentCaptor2 = ArgumentCaptor.forClass(String.class)
        verify(git).commit(argumentCaptor2.capture(), eq('productionName'), anyString())
        assertThat(argumentCaptor2.getValue()).isEqualTo('[production] production/reponame@2b')

        argumentCaptor2 = ArgumentCaptor.forClass(String.class)
        verify(git).commit(argumentCaptor2.capture(), eq('qaName'), anyString())
        assertThat(argumentCaptor2.getValue()).isEqualTo('[qa] qa/reponame@3c')

        verify(git, times(3)).pushAndPullOnFailure(anyString())

        assertThat(deployViaGitops.currentBuild.description).isEqualTo('GitOps commits: staging (1234abcd); production (1234abcd); qa (1234abcd)\nImage: [newImageName]')
    }

    @Test
    void 'no changes in git with multiple stages'() {
        when(git.areChangesStagedForCommit()).thenReturn(false)

        gitRepo.use {
            deployViaGitops.call(gitopsConfig(multipleStages))
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
            deployViaGitops.call(gitopsConfig(singleStages))
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
        def output = deployViaGitops.createBuildDescription('changes', "imageName")
        assert output == 'GitOps commits: changes\nImage: imageName'
    }

    @Test
    void 'return No Changes if no changes are present'() {
        def output = deployViaGitops.createBuildDescription('', 'imageName')
        assert output == 'GitOps commits: No changes\nImage: imageName'
    }

    @Test
    void 'changes are being aggregated'() {
        def changes = ['1', '2', '3']
        def output = deployViaGitops.aggregateChangesOnGitOpsRepo(changes)
        assert output == '1; 2; 3'
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
