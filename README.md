
# gitops-build-lib

## Features

* Write Kubernetes resources to a git repo (for a GitOps operator to pick them up)
* Support for multiple stages
  * Push to application branch and create PR (staging) or
  * Push to production branch (e.g. "main") directly
* Write image tag into kubernetes deplyoments
* Fail early: Validate YAML syntax (yamllint) and against Kubernetes schema (kubeval)
* TODO

## Usage

TODO separate mandatory from optional fields
```
gitopsConfig = [
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
    ],
    validators: [
        kubeval: [
            validator: new Kubeval(this),
            enabled: true,
            config: [
                // We use the helm image (that also contains kubeval plugin) to speed up builds by allowing to reuse image
                image: helmImage,
                k8sSchemaVersion: '1.18.1'
            ]
        ],
        yamllint: [
            validator: new Yamllint(this),
            enabled: true,
            config: [
                image: 'cytopia/yamllint:1.25-0.7',
                // Default to relaxed profile because it's feasible for mere mortalYAML programmers.
                // It still fails on syntax errors.
                profile: 'relaxed'
            ]
        ]
    ]
]

deployViaGitops(gitopsConfig)
```
