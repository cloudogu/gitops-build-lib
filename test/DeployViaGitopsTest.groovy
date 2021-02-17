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
                mainBranch        : 'mainBranch',
                updateImages      : [
                        [deploymentFilename: "deployment.yaml",
                         containerName     : 'application',
                         imageName         : 'imageName']
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

//        script.cesBuildLib = cblMock

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
          image: 'testImage'
'''

        when(git.areChangesStagedForCommit()).thenReturn(true)
        when(git.getRepositoryUrl()).thenReturn("repo/url/my/new/repo")

        deployViaGitops.metaClass.readYaml = {
            return new YamlSlurper().parseText(configYaml)
        }

        deployViaGitops.metaClass.writeYaml = {
            return null
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        // always reset metaClass after messing with it to prevent changes from leaking to other tests
        deployViaGitops.metaClass = null
    }

    @Test
    void 'single stage deployment via gitops'() {

        deployViaGitops(gitopsConfig(singleStages))
        assertThat(helper.callStack.findAll { call ->call.methodName == "dir"}.any { call ->
            callArgsToString(call).contains(".configRepoTempDir")
        }).isTrue()

        ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(git).call(argumentCaptor.capture())
        assertThat(argumentCaptor.getValue().url).isEqualTo 'configRepositoryUrl'
        assertThat(argumentCaptor.getValue().branch).isEqualTo('mainBranch')

        verify(git, times(1)).fetch()

        println("bla")
        println(deployViaGitops.currentBuild.description)
    }
    @Test
    void 'multi-stage deployment via GitOps'() {

//        def expectedKubeShStringArgs = ["kubeval",  "-d ${targetDirectory} -v ${k8sVersion} --strict"]
    }

    @Test
    void 'no changes in git'() {

    }


//    @Test
//    void 'preparing gitRepo returns config'() {
//        def stub = new StubFor(GitRepo)
//
//        stub.demand.with {
//            create { new GitRepo('test', 'test', 'test', 'test', 'test') }
//        }
//
//        stub.use {
//            def output = script.prepareGitRepo(cblMock.Git.new)
//            assert output.applicationRepo != null
//            assert output.configRepoTempDir == '.configRepoTempDir'
//        }
//    }
//
    @Test
    void 'returns correct build description'() {
        def output = script.createBuildDescription('changes', "imageName")
        assert output == 'GitOps commits: changes\nImage: imageName'
    }

    @Test
    void 'return No Changes if no changes are present'() {
        def output = script.createBuildDescription('', 'imageName')
        assert output == 'GitOps commits: No changes\nImage: imageName'
    }

    @Test
    void 'changes are being aggregated'() {
        def changes = ['1', '2', '3']
        def output = script.aggregateChangesOnGitOpsRepo(changes)
        assert output == '1; 2; 3'
    }
//
//    @Test
//    void 'sync gitops repo runs three times for three stages'() {
//        def gitMock = mock(Git.class)
//
////        script.metaClass.syncGitopsRepo { return 'changes' }
//
//        helper.registerAllowedMethod("syncGitopsRepo", [String, String, Object, Map, Map], { String stage, String branch, Object git, Map gitRepo, Map gitopsConfig ->
//            return "changes for ${stage}"
//        })
//
//        helper.registerAllowedMethod("createPullRequest", [Map, String, String], { Map gitopsConfig, String stage, String branch ->
//            null
//        })
//
//
//        def output = script.syncGitopsRepoPerStage(GITOPS_CONFIG, gitMock, GIT_REPO)
//        println(output)
//
//    }
//
//    @Test
//    void 'successful commit yields commit message'() {
//        given:
//        Git gitMock = mock(Git.class)
//        Mockito.when(gitMock.areChangesStagedForCommit()).thenReturn(true)
//        Mockito.doNothing().when(gitMock).commit(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())
////        Mockito.when(gitMock.commit(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(null)
//        Mockito.doNothing().when(gitMock).pushAndPullOnFailure(Mockito.anyString())
////        Mockito.when(gitMock.pushAndPullOnFailure(Mockito.anyString())).thenReturn(null)
////        def git = new MockFor(Git)
////        git.demand.with {
////            areChangesStagedForCommit { return true }
////        }
//
//        when:
//        def output = script.commitAndPushToStage('staging', 'main', gitMock, GIT_REPO)
//
//        then:
//        //            1 * git.commit(_)
//        1 * git.pushAndPullOnFailure('origin staging')
//        println(output)
//    }


    private static void setupGlobals(Script script) {
        script.metaClass.getAppDockerRegistry = { EXPECTED_REGISTRY }
        script.application = EXPECTED_APPLICATION
        script.repoDigest = null
        script.forceDeployStaging = null
        script.reDeployImageVersion = null
        script.skipQualityAssurance = null
        script.configRepositoryPRUrl = "scm/repo"
    }

}
