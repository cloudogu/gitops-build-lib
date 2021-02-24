package com.cloudogu.gitopsbuildlib

import com.cloudogu.ces.cesbuildlib.DockerMock

class ScriptMock {

    DockerMock dockerMock = new DockerMock()

    List<String> actualShArgs = new LinkedList<>()
    List<String> actualEchoArgs = new LinkedList<>()

    def mock =
        [
            docker: dockerMock.createMock(),
            pwd   : { 'pwd' },
            sh    : { args -> actualShArgs += args.toString() },
            echo  : { args -> actualEchoArgs += args.toString() },
            env   : [
                WORKSPACE: 'workspace'
            ]
        ]
}
