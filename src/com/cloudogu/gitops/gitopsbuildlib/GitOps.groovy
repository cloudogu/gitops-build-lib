package com.cloudogu.gitops.gitopsbuildlib

class GitOps implements Serializable{
  private script
  private cesBuidLib

  GitOps(script, cesBuildLibRepo, cesBuildLibVersion) {
    this.script = script
    initCesBuildLib(cesBuildLibRepo, cesBuildLibVersion)
  }

  private initCesBuildLib(cesBuildLibRepo, cesBuildLibVersion) {
    this.cesBuidLib = library(identifier: "ces-build-lib@${cesBuildLibVersion}",
        retriever: modernSCM([$class: 'GitSCMSource', remote: cesBuildLibRepo])
    ).com.cloudogu.ces.cesbuildlib
  }
}
