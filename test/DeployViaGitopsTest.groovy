import com.cloudogu.ces.cesbuildlib.Docker
import com.cloudogu.gitopsbuildlib.GitRepo
import com.cloudogu.ces.cesbuildlib.Git
import com.lesfurets.jenkins.unit.BasePipelineTest
import groovy.mock.interceptor.MockFor
import groovy.mock.interceptor.StubFor
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito

import static org.mockito.Mockito.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeployViaGitopsTest extends BasePipelineTest {

    class CblMock {
        class Git {

        }
        def Docker = {}
    }

    def script

    static final String EXPECTED_OUTPUT = "test"

    static final String EXPECTED_APPLICATION = 'app'

    static final Map GIT_REPO = [
            applicationRepo: 'applicationRepo',
            configRepoTempDir: 'configRepoTempDir'
    ]

    static final Map GITOPS_CONFIG = [
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
            stages            : [
                    staging   : [deployDirectly: true],
                    production: [deployDirectly: false],
                    qa        : []
            ]
    ]

    @BeforeAll
    void setUp() throws Exception {
        scriptRoots += 'vars'
        super.setUp()
    }

    @BeforeEach
    void init() {
        script = loadScript('vars/deployViaGitops.groovy')
        binding.getVariable('currentBuild').result = 'SUCCESS'
        setupGlobals(script)
    }

    @Test
    void 'test'() {
        def cblMock = new CblMock()
        def gitMock = mock(Git.class)
        def docker = mock(Docker.class)
        cblMock.Docker.metaClass.new = {
            return docker
        }
        script.cesBuildLib = cblMock

        def output = script.syncGitopsRepoPerStage(GITOPS_CONFIG, gitMock, GIT_REPO)
        println(output)

//        def test = DeployViaGitopsTest.cblMock.Git.new(this)
//        println(test)
    }

//    @Test
//    void 'preparing gitRepo returns config'() {
//        def git = mock(Git.class)
//        def stub = new StubFor(GitRepo)
//
//        stub.demand.with {
//            create { new GitRepo('test', 'test', 'test', 'test', 'test') }
//        }
//
//        stub.use {
//            def output = script.prepareGitRepo(git)
//            assert output.applicationRepo != null
//            assert output.configRepoTempDir == '.configRepoTempDir'
//        }
//    }
//
//    @Test
//    void 'returns correct build description'() {
//        def output = script.createBuildDescription('changes', "imageName")
//        assert output == 'GitOps commits: changes\nImage: imageName'
//    }
//
//    @Test
//    void 'return No Changes if no changes are present'() {
//        def output = script.createBuildDescription('', 'imageName')
//        assert output == 'GitOps commits: No changes\nImage: imageName'
//    }
//
//    @Test
//    void 'changes are being aggregated'() {
//        def changes = ['1', '2', '3']
//        def output = script.aggregateChangesOnGitOpsRepo(changes)
//        assert output == '1; 2; 3'
//    }
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
