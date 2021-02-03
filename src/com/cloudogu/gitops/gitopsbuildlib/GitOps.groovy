package com.cloudogu.gitops.gitopsbuildlib

class GitOps implements Serializable{
  private script
  private cesBuidLib

  GitOps(script, cesBuildLibRepo, cesBuildLibVersion) {
    this.script = script
    initCesBuildLib(cesBuildLibRepo, cesBuildLibVersion)
  }

  private initCesBuildLib(cesBuildLibRepo, cesBuildLibVersion) {
    this.cesBuidLib = script.library(identifier: "ces-build-lib@${cesBuildLibVersion}",
        retriever: script.modernSCM([$class: 'GitSCMSource', remote: cesBuildLibRepo])
    ).com.cloudogu.ces.cesbuildlib
  }
}
