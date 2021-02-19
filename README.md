# gitops-build-lib

Jenkins pipeline shared library for automating deployments via GitOps

## Features

* Write Kubernetes resources to a git repo (for a GitOps operator to pick them up)
* Support for multiple stages within the same gitOps Repo
    * Push to application branch and create PR (staging) or
    * Push to production branch (e.g. "main") directly
* Write image tag into kubernetes deployments
* Fail early: Validate YAML syntax (yamllint) and against Kubernetes schema (kubeval)
* Pluggable validators: Simply add your own validators
* TODO

## Usage

// TODO make SCM pluggable as well

```groovy
gitopsConfig = [
    // Mandatory properties
    scmmCredentialsId : 'scmManagerCredentials',
    scmmConfigRepoUrl : 'configRepositoryUrl',
    scmmPullRequestUrl: 'configRepositoryPRUrl',
    application       : 'application',
    // stages consists of [ stageName: [ deployDirectly: true ]]
    // stageName is mapped to a folder in the gitops repo
    // deployDirectly: true  -> deploys directly
    // deployDirectly: false -> creates a PR (default)
    stages            : [
        staging   : [deployDirectly: true],
        production: [deployDirectly: false],
        qa        : []
    ],

    // Optional properties (can be overwritten)

    cesBuildLibRepo   : 'https://github.com/cloudogu/ces-build-lib',
    cesBuildLibVersion: '1.45.0',
    mainBranch        : 'main',
    // Default is empty list
    updateImages      : [
        [deploymentFilename: "deployment.yaml",
         containerName     : 'application',
         imageName         : 'imageName']
    ],
    validators        : [
        // You can disable a validator by just setting the appropriate "enabled" propery to false
        // You can add your own validator here, see `Kubeval.groovy` for an example
        kubeval : [
            validator: new Kubeval(this),
            enabled  : true,
            config   : [
                // We use the helm image (that also contains kubeval plugin) to speed up builds by allowing to reuse image
                image           : 'ghcr.io/cloudogu/helm:3.4.1-1',
                k8sSchemaVersion: '1.18.1'
            ]
        ],
        yamllint: [
            validator: new Yamllint(this),
            enabled  : true,
            config   : [
                image  : 'cytopia/yamllint:1.25-0.7',
                // Default to relaxed profile because it's feasible for mere mortalYAML programmers.
                // It still fails on syntax errors.
                profile: 'relaxed'
            ]
        ]
    ]
]

deployViaGitops(gitopsConfig)
```
