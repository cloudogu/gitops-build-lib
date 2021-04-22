package com.cloudogu.gitopsbuildlib

import groovy.yaml.YamlSlurper

class ScriptMock {

    DockerMock dockerMock = new DockerMock()
    GitMock gitMock = new GitMock()

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
                    new: { args -> return gitMock.createMock() },
                    fetch: { gitMock.setFetch() },
                    checkout: { args -> gitMock.actualCheckoutArgs(args) }
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
