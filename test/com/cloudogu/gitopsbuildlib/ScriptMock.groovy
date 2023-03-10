package com.cloudogu.gitopsbuildlib

import com.cloudogu.ces.cesbuildlib.Git
import groovy.yaml.YamlSlurper

import static org.mockito.Mockito.mock

class ScriptMock {

    DockerMock dockerMock = new DockerMock()
    Git gitMock = mock(Git.class)

    List<String> actualShArgs = new LinkedList<>()
    List<String> actualEchoArgs = new LinkedList<>()
    List<String> actualReadYamlArgs = new LinkedList<>()
    List<String> actualGitArgs = new LinkedList<>()
    List<String> actualDir = new LinkedList<>()
    def configYaml = '''\
---
#this part is only for PlainTest regarding updating the image name
spec:
  template:
    spec:
      containers:
        - name: 'application'
          image: 'oldImageName'
#this part is only for HelmTest regarding changing the yaml values
to:
  be:
    changed: 'oldValue'
'''
    List<String> actualWriteYamlArgs = new LinkedList<>()
    List<String> actualReadFileArgs = new LinkedList<>()
    List<String> actualWriteFileArgs = new LinkedList<>()

    def mock =
        [
            cesBuildLib: [
                Docker: [
                    new: { args -> return dockerMock.createMock() }
                    ],
                Git: [
                    new: { args -> return gitMock },
                    ],
                ],
            docker: dockerMock.createMock(),
            git: { args -> actualGitArgs += args.toString() },
            pwd   : { 'pwd' },
            sh    : { args -> actualShArgs += args.toString() },
            echo  : { args -> actualEchoArgs += args.toString() },
            readYaml: { args -> actualReadYamlArgs += args.toString(); return new YamlSlurper().parseText(configYaml) },
            writeYaml: { args -> actualWriteYamlArgs += args.toString() },
            readFile : { args -> actualReadFileArgs += args.toString(); return configYaml},
            writeFile: { args -> actualWriteFileArgs += args.toString() },
            env   : [
                WORKSPACE: 'workspace'
            ],
            dir: { dir, closure -> println(dir); actualDir += dir.toString(); return closure.call() }
        ]
}
