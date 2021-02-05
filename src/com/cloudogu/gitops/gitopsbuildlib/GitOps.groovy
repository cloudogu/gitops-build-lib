package com.cloudogu.gitops.gitopsbuildlib

class GitOps implements Serializable{
  // "Constants


  private script
  private cesBuildLib
  private scmManagerCredentials
  private application

  GitOps(script, application, cesBuildLibRepo, cesBuildLibVersion, scmManagerCredentials) {
    this.script = script
    this.application = application
    initCesBuildLib(cesBuildLibRepo, cesBuildLibVersion)
    this.scmManagerCredentials = scmManagerCredentials
  }



  