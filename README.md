
# gitops-build-lib
```
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
```
