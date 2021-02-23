# gitops-build-lib

Jenkins pipeline shared library for automating deployments via GitOps

## Features

* Write Kubernetes resources to a git repo (for a GitOps operator to pick them up)
* Support for multiple stages within the same gitOps Repo
    * Push to application branch and create PR (staging) or
    * Push to production branch (e.g. "main") directly
* Deployment methods
  * Plain Kubernetes resources - write image tag into kubernetes deployments dynamically
  * Helm deployments - set values (e.g. image tag) dynamically   
* Fail early: Validate YAML syntax (yamllint) and against Kubernetes schema (kubeval)
* Pluggable validators: Simply add your own validators
* SCM Systems Supported
  * SCM-Manager
  * Abstraction for others is WIP  

## Usage

TODO describe how to use, conventions and folder input and output 

* Stages
* Plain 
* Helm
  Conventions: 
  * Stage name -> `${deployments.path}/values-<stage>.yaml` + `${deployments.path}/values-common.yaml`
  * Helm Release name = `${application}` (Flux, not used for argo, because we create plain ressources using `helm template`)

```groovy
gitopsConfig = [
    // Mandatory properties
    scmmCredentialsId : 'scmManagerCredentials',
    scmmConfigRepoUrl : 'configRepositoryUrl',
    scmmPullRequestBaseUrl  : 'configRepositoryPRBaseUrl',
    scmmPullRequestRepo     : 'configRepositoryPRRepo',
    application       : 'application',
    // stages consists of [ stageName: [ deployDirectly: true ]]
    // stageName is mapped to a folder in the gitops repo
    // deployDirectly: true  -> deploys directly
    // deployDirectly: false -> creates a PR (default)
    stages            : [
        staging   : [deployDirectly: true ],
        production: [deployDirectly: false],
        qa        : []
    ],
    deployments :[
        sourcePath: 'k8s', // path of k8s resources in application repository. Default: 'k8s'
        // Either "plain" or "helm" is mandatory
        plain: [
            updateImages      : [
                [filename: "deployment.yaml", // relative to deployments.path
                 containerName     : 'application',
                 imageName         : 'imageName']
            ]
        ],
        helm: [
            repoType : 'GIT', 
            repoUrl  : "https://git-repo/namespace/name",
            credentialsId : 'creds',
            version  : '1.2.3', // tag, commit or branch
            chartPath: 'chart', 
            extraResources: ['config, secrets'], // files or folders relative to deployments.path 
            updateValues: [ [fieldPath: "image.name", newValue: imageName] ]
        ], 
        // Future alternative: Choose between HELM or GIT as chart repo
        helm: [
            repoType : 'HELM',
            repoUrl  : 'https://charts.bitnami.com/bitnami',
            credentialsId : 'creds',
            version  : '7.1.6',
            chartName: 'nginx',
            extraResources: ['config, secrets'], // files or folders relative to deployments.path. Default empty array. 
            updateValues: [ [fieldPath: "image.name", newValue: imageName] ]
        ]
        // Additional future alternative
        // kustomize: []
    ],

    // Optional properties (can be overwritten)

    cesBuildLibRepo   : 'https://github.com/cloudogu/ces-build-lib',
    cesBuildLibVersion: '1.45.0',
    mainBranch        : 'main',
    validators        : [
        // You can disable a validator by just setting the appropriate "enabled" propery to false
        // You can add your own validator here, see `Kubeval.groovy` for an example
        yamllint: [
            validator: new Yamllint(this),
            enabled  : true,
            config   : [
                image  : 'cytopia/yamllint:1.25-0.7',
                // Default to relaxed profile because it's feasible for mere mortalYAML programmers.
                // It still fails on syntax errors.
                profile: 'relaxed'
            ]
        ],
        kubeval : [
            validator: new Kubeval(this),
            enabled  : true,
            config   : [
                // We use the helm image (that also contains kubeval plugin) to speed up builds by allowing to reuse image
                image           : 'ghcr.io/cloudogu/helm:3.4.1-1',
                k8sSchemaVersion: '1.18.1'
            ]
        ],
        helmKubeval: [
            validator: new HelmKubeval(this),
            // Skips automatically when "deployments.helm" is not set
            enabled  : true,
            config   : [
                image           : 'ghcr.io/cloudogu/helm:3.4.1-1',
                k8sSchemaVersion: '1.18.1'
            ]
        ],
    ]
]

deployViaGitops(gitopsConfig)
```

## Examples

The first evolution of this library was extracted from our [GitOps Playground](https://github.com/cloudogu/k8s-gitops-playground).

There you will find example on how to use this library:
* [fluxv1/plain-k8s](https://github.com/cloudogu/k8s-gitops-playground/blob/main/applications/petclinic/fluxv1/plain-k8s/Jenkinsfile)
