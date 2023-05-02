package com.cloudogu.gitopsbuildlib.deployment

import com.cloudogu.gitopsbuildlib.ScriptMock
import com.cloudogu.gitopsbuildlib.deployment.plain.Plain
import com.cloudogu.gitopsbuildlib.validation.HelmKubeval
import com.cloudogu.gitopsbuildlib.validation.Kubeval
import com.cloudogu.gitopsbuildlib.validation.Yamllint
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat 

class PlainTest {

    def scriptMock = new ScriptMock()
    def plain = new Plain(scriptMock.mock, [
        application: 'app',
        gitopsTool: 'FLUX',
        deployments: [
            sourcePath: 'k8s',
            destinationRootPath: '.',
            plain: [
                updateImages: [
                    [filename     : "deployment.yaml", // relative to deployments.path
                     containerName: 'application',
                     imageName    : 'imageNameReplacedTest']
                ]
            ]
        ],
        folderStructureStrategy: 'GLOBAL_ENV',
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
    ])

    String deploymentYaml = '''
kind: Deployment
spec:
  template:
    spec:
      containers:
        - name: 'application'
          image: 'oldImageName'
'''

    String cronJobYaml = '''
kind: CronJob
spec:
  jobTemplate:
    spec:
      template:
        spec:
          containers:
            - name: 'application'
              image: 'oldImageName'
            - name: 'other'
              image: 'otherImageName'
'''

    @BeforeEach
    void init () {
        scriptMock.configYaml = deploymentYaml
    }
    
    @Test
    void 'successful update'() {

        plain.preValidation('staging')
        assertThat(scriptMock.actualReadYamlArgs[0]).isEqualTo('[file:staging/app/deployment.yaml]')
        assertThat(scriptMock.actualWriteYamlArgs[0]).isEqualTo('[file:staging/app/deployment.yaml, data:[kind:Deployment, spec:[template:[spec:[containers:[[image:imageNameReplacedTest, name:application]]]]]], overwrite:true]')
    }

    @Test
    void 'successful update with statefulSet'() {
        scriptMock.configYaml = scriptMock.configYaml.replace('kind: Deployment', 'kind: StatefulSet')
        plain.preValidation('staging')
        assertThat(scriptMock.actualReadYamlArgs[0]).isEqualTo('[file:staging/app/deployment.yaml]')
        assertThat(scriptMock.actualWriteYamlArgs[0]).isEqualTo('[file:staging/app/deployment.yaml, data:[kind:StatefulSet, spec:[template:[spec:[containers:[[image:imageNameReplacedTest, name:application]]]]]], overwrite:true]')
    }
    
    @Test
    void 'successful update with cronjob'() {
        scriptMock.configYaml = cronJobYaml

        plain.preValidation('staging')
        assertThat(scriptMock.actualReadYamlArgs[0]).isEqualTo('[file:staging/app/deployment.yaml]')
        assertThat(scriptMock.actualWriteYamlArgs[0]).isEqualTo('[file:staging/app/deployment.yaml, data:[kind:CronJob, spec:[jobTemplate:[spec:[template:[spec:[containers:[[image:imageNameReplacedTest, name:application], [image:otherImageName, name:other]]]]]]]], overwrite:true]')
    }

    @Test
    void 'successful update with other resource'() {
        scriptMock.configYaml = scriptMock.configYaml.replace('kind: Deployment', 'kind: SomethingElse')
        plain.preValidation('staging')
        assertThat(scriptMock.actualReadYamlArgs[0]).isEqualTo('[file:staging/app/deployment.yaml]')
        assertThat(scriptMock.actualWriteYamlArgs[0]).isEqualTo('[file:staging/app/deployment.yaml, data:[kind:SomethingElse, spec:[template:[spec:[containers:[[image:imageNameReplacedTest, name:application]]]]]], overwrite:true]')
        assertThat(scriptMock.actualEchoArgs).contains('Warning: Kind \'SomethingElse\' is unknown, using best effort to find \'containers\' in YAML')
    }
    
    @Test
    void 'successful update with ENV_PER_APP and other destinationRootPath '() {
        plain.gitopsConfig['folderStructureStrategy'] = 'ENV_PER_APP'
        plain.gitopsConfig['deployments']['destinationRootPath'] = 'apps'

        plain.preValidation('staging')
        assertThat(scriptMock.actualReadYamlArgs[0]).isEqualTo('[file:apps/app/staging/deployment.yaml]')
        assertThat(scriptMock.actualWriteYamlArgs[0]).isEqualTo('[file:apps/app/staging/deployment.yaml, data:[kind:Deployment, spec:[template:[spec:[containers:[[image:imageNameReplacedTest, name:application]]]]]], overwrite:true]')
    }

    @Test
    void 'flux plain validates with yamllint and kubeval'() {
        plain.validate('staging')

        assertThat(scriptMock.actualEchoArgs[0]).isEqualTo('Starting validator Yamllint for FLUX in PLAIN resources')
        assertThat(scriptMock.actualEchoArgs[1]).isEqualTo('Starting validator Kubeval for FLUX in PLAIN resources')
        assertThat(scriptMock.actualEchoArgs[2]).isEqualTo('Skipping validator HelmKubeval because it is configured as enabled=false or doesn\'t support the given gitopsTool=FLUX or sourceType=PLAIN')

        assertThat(scriptMock.actualShArgs[0]).isEqualTo('yamllint -f standard staging/app')
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('kubeval -d staging/app -v null --strict --ignore-missing-schemas')
    }

    @Test
    void 'argo plain validates with yamllint and kubeval'() {
        plain.gitopsConfig['gitopsTool'] = 'ARGO'
        plain.validate('staging')

        assertThat(scriptMock.actualEchoArgs[0]).isEqualTo('Starting validator Yamllint for ARGO in PLAIN resources')
        assertThat(scriptMock.actualEchoArgs[1]).isEqualTo('Starting validator Kubeval for ARGO in PLAIN resources')
        assertThat(scriptMock.actualEchoArgs[2]).isEqualTo('Skipping validator HelmKubeval because it is configured as enabled=false or doesn\'t support the given gitopsTool=ARGO or sourceType=PLAIN')

        assertThat(scriptMock.actualShArgs[0]).isEqualTo('yamllint -f standard staging/app')
        assertThat(scriptMock.actualShArgs[1]).isEqualTo('kubeval -d staging/app -v null --strict --ignore-missing-schemas')
    }
}
