# gitops-build-lib

Jenkins pipeline shared library for automating deployments via GitOps. Take a look at
[cloudogu/k8s-gitops-playground](https://github.com/cloudogu/k8s-gitops-playground) to see a fully working example bundled
with the complete infrastructure for a gitops deep dive.

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
# Table of contents

---
- [Features](#features)
- [Examples](#examples)
- [Usage](#usage)
- [Default Structure](#default-structure)
    - [FluxV1](#fluxv1)
        - [Plain-k8s](#plain-k8s)
        - [Helm](#helm)
    - [Flux V2 (Upcoming)](#fluxv2-upcoming)    
    - [ArgoCD (Upcoming)](#argocd-upcoming)
- [GitOps-Config](#gitops-config)
    - [Properties](#properties)
- [Stages](#stages)
    - [Namespaces](#namespaces)
        - [Important Note](#important-note)
    - [Conventions](#conventions)
- [Deployment](#deployment)
    - [Plain k8s](#plain-k8s-deployment)
    - [Helm](#helm-deployment)
        - [Conventions](#conventions-for-helm-deployment)
- [Validators](#validators)
    - [Custom validators](#custom-validators)
- [Extra Files](#extra-files)


<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Features

* Write Kubernetes resources to a git repo (for a GitOps operator to pick them up)
* Support for multiple stages within the same gitOps Repo
    * Push to application branch and create PR (production) or
    * Push to main branch (e.g. "main") directly for staging deployment
* Deployment methods
    * Plain Kubernetes resources - write image tag into kubernetes deployments dynamically
        * add files to deployment which will be generated to a configmap
    * Helm deployments - set values (e.g. image tag) dynamically
        * add custom k8s resources

* Support for different Helm-Repository types
    * Helm
    * Git
* Fail early: Validate YAML syntax (yamllint) and against Kubernetes schema (kubeval) as well as Helm charts (helmKubeval)
* Pluggable validators: Simply add your own validators
* SCM Systems Supported
    * SCM-Manager
    * Abstraction for others is WIP

---

## Examples

Example of a small and yet complete **gitops-config** for a helm-deployment of an application. This would lead to a deployment of your staging environment
by updating the resources of "staging" folder within your gitops-folder in git. For production it will open a PR with the changes.
Detailed instructions about the attributes can be found [here](#gitops-config).

```groovy
def gitopsConfig = [
    scmmCredentialsId : <scmCredentialsId>,
    scmmConfigRepoUrl : <configRepositoryUrl>,
    scmmPullRequestBaseUrl: <configRepositoryPRBaseUrl>,
    scmmPullRequestRepo: <configRepositoryPRRepo>,
    cesBuildLibRepo: <cesBuildLibRepo>,
    cesBuildLibVersion: <cesBuildLibVersion>,
    application: <applicationName>,
    mainBranch: <branchName>,
    deployments: [
        sourcePath: 'k8s',
        helm : [
            repoType : 'HELM',
            credentialsId : 'creds',
            repoUrl  : <helmChartRepository>,
            chartName: <helmChartName>,
            version  : <helmChartVersion>,
            updateValues  : [[fieldPath: "image.name", newValue: imageName]]
        ]
    ],
    stages: [
        staging: [ 
            namespace: 'my-staging',
            deployDirectly: true
        ],
        production: [
            namespace: 'my-production',
            deployDirectly: false 
        ],
    ]
]

deployViaGitops(gitopsConfig)
```

**FluxV1:**
* [using petclinic  with helm and no validators](https://github.com/cloudogu/k8s-gitops-playground/blob/main/applications/petclinic/fluxv1/helm/Jenkinsfile)
* [using petclinic with plain-k8s and no validators](https://github.com/cloudogu/k8s-gitops-playground/blob/main/applications/petclinic/fluxv1/plain-k8s/Jenkinsfile)
* [using helm with nginx and extra resources](https://github.com/cloudogu/k8s-gitops-playground/blob/main/applications/nginx/fluxv1/Jenkinsfile)

---

## Usage

At first you have to import the library. You can either use one of the following options:
* `library()` when the library is being loaded from a private git
* `@Library` works out of the box with [pipeline-github-lib plugin](https://plugins.jenkins.io/pipeline-github-lib/) to load the library from github

First option can be used when the build-lib is mirrored or cloned onto your own git. You only need to provide the
remote url and the desired version.

```groovy
gitOpsBuildLib = library(identifier: "gitops-build-lib@<gitOpsBuildLibVersion>",
    retriever: modernSCM([$class: 'GitSCMSource', remote: <gitOpsBuildLibRepo>])
).com.cloudogu.gitops.gitopsbuildlib
```

If you can access the internet or just dont want to mirror the repo you can just import it directly from github using the [pipeline-github-lib plugin](https://plugins.jenkins.io/pipeline-github-lib/) plugin.
```groovy
@Library('github.com/cloudogu/gitops-build-lib@0.0.5')
import com.cloudogu.gitops.gitopsbuildlib.*
```

To utilize the library itself, there is little to do:
- Setup a [`gitopsConfig`](#examples) containing the following sections
    - properties (e.g. remote urls to scm, application etc.)
    - stages
    - deployments
    - validators
    - fileConfigMaps
- Call `deployViaGitops(gitopsConfig)`

---

## Default Structure
A default project structure could look like the examples below. Make sure you have your k8s and/or helm resources bundled
in a folder. This specific resources folder (here k8s) will later be specified by the `sourcePath` within the deployments section of your `gitopsConfig`.

#### FluxV1

#### Plain-k8s
```
├── application
    ├── config.yamllint.yaml // not necessarily needed
    ├── Jenkinsfile
    └── k8s
        ├── production
        │   ├── deployment.yaml
        │   └── service.yaml
        └── staging
            ├── deployment.yaml
            └── service.yaml
```

#### Helm
```
├── application
    ├── config.yamllint.yaml // not necessarily needed
    ├── Jenkinsfile
    └── k8s
        ├── production
        │   ├── configMap.yaml
        │   └── secrets
        │       ├── secret2.yaml
        │       └── secret.yaml
        ├── staging
        │   ├── configMap.yaml
        │   └── secrets
        │       ├── secret2.yaml
        │       └── secret.yaml
        ├── values-production.yaml
        ├── values-shared.yaml
        └── values-staging.yaml
```

### FluxV2 (Upcoming)

### ArgoCD (Upcoming)

---

## GitOps-Config
The gitops config is basically a groovy map following conventions to have a descriptive way of defining deployments. You can find a complete yet simple example [here](#examples).

### Properties
First of all there are some mandatory properties e.g. the information about your gitops repository and the application repository.

```
scmmCredentialsId:      '<scmManagerCredentialsId>',
scmmConfigRepoUrl:      '<configRepositoryUrl>',        # this is your full gitops repo url e.g. http://scmm-scm-manager/scm/repo/fluxv1/gitops
scmmPullRequestBaseUrl: '<configRepositoryPRBaseUrl>',  # this is your gitops base url      e.g. http://scmm-scm-manager/scm
scmmPullRequestRepo:    '<configRepositoryPRRepo>',     # this is the gitops repo           e.g. fluxv1/gitops
application:            '<applicationName>'
```

and some optional parameters (below are the defaults) for the configuration of the dependency to the ces-build-lib or the default name for the git branch:
```
cesBuildLibRepo:    'https://github.com/cloudogu/ces-build-lib',
cesBuildLibVersion: '1.45.0',
mainBranch:         'main'
```
---

## Stages
The GitOps-build-lib supports builds on multiple stages. A stage is defined by a name and contains a namespace (used to
generate the resources) and a deployment-flag.

```groovy
def gitopsConfig = [
        stages: [
            staging: [
                namespace: 'staging',
                deployDirectly: true
            ],
            production: [
                namespace: 'production',
                deployDirectly: false
            ]
        ]
]
```

If it is set to deploy directly it will commit and push to your desired `gitops-folder` and therefore triggers a deployment. If it is set to false
it will create a PR on your `gitops-folder`. **Remember** there are [ipmortant](#important-note) conventions regarding namespaces and the folder structure.

### Namespaces

You need to specify a namespace for each stage for Helm deployments. For Plain you just need it if you add [extra Files](#extra-files).
If no namespace is specified, the library uses the `gitopsConfig.stages.${yourStage}` as the namespace. But beware if you use special characters then you need to use single ticks:
```groovy
def gitopsConfig = [
        stages: [
                'xx-staging': [],
                'xx-production': []
        ]
] 
```

Otherwise every stage can be defined with an additional `namespace` property:
```groovy
def gitopsConfig = [
        stages: [
                staging: [
                        namespace: 'xx-staging'
                ],
                production: [
                        namespace: 'xx-production'
                ]
        ]
] 
```

#### Important note
Under your `gitopsConfig.deployments.sourcePath` (here k8s) the subfolders have to be named exactly as the stages under `gitopsConfig.stages`. As well as the values files if you have a Helm application.
So if you have a config looking like:
```groovy
def gitopsConfig = [
        stages: [
                'xx-staging': [],
                'xx-production': []
        ]
] 
```
Then your folder structure has to look like:
For Plain:
```
├── application
    ├── config.yamllint.yaml // not necessarily needed
    ├── Jenkinsfile
    └── k8s
        ├── xx-production
        │   ├── deployment.yaml
        │   └── service.yaml
        └── xx-staging
            ├── deployment.yaml
            └── service.yaml
```

For Helm:
```
├── application
    ├── config.yamllint.yaml // not necessarily needed
    ├── Jenkinsfile
    └── k8s
        ├── xx-production
        │   ├── {extraResources}
        └── xx-staging
            ├── {extraResources}
        ├── values-shared.yaml
        ├── values-xx-production.yaml
        ├── values-xx-staging.yaml
```

Or if you use namespaces:
```groovy
def gitopsConfig = [
        stages: [
                staging: [
                        namespace: 'xx-staging'
                ],
                production: [
                        namespace: 'xx-production'
                ]
        ]
] 
```
Then your folder structure has to look like:

For Plain:
```
├── application
    ├── config.yamllint.yaml // not necessarily needed
    ├── Jenkinsfile
    └── k8s
        ├── production
        │   ├── deployment.yaml
        │   └── service.yaml
        └── staging
            ├── deployment.yaml
            └── service.yaml
```

For Helm
```
├── application
    ├── config.yamllint.yaml // not necessarily needed
    ├── Jenkinsfile
    └── k8s
        ├── production
        │   ├── {extraResources}
        └── staging
            ├── {extraResources}
        ├── values-shared.yaml
        ├── values-production.yaml
        ├── values-staging.yaml
```

### Conventions for stages
- Stage name is used to identify the yamls to be used:
  > `${deployments.sourcePath}/values-<stage>.yaml` + `${deployments.sourcePath}/values-shared.yaml`
  For now we only support one `values-shared.yaml` which will be used for all namespaces and one additional values file for each namespace `values-${stage}.yaml`
---

## Deployment
The deployment has to contain the path of your k8s resources within the application and either a config section for `plain-k8s` resources or for `helm` resources.

```groovy
def gitopsConfig = [
        deployments: [
            sourcePath: 'k8s', // path of k8s resources in application repository. Default: 'k8s'
            // Either "plain" or "helm" is mandatory
            plain: [], // use plain if you only have, as the name suggests, plain k8s resources
            helm: [] // or if you want to deploy a helm release use `helm`
        ]
```

### Plain k8s deployment
You can utilize the build-lib to enable builds based on plain k8s resources

```groovy
def gitopsConfig = [
        deployments: [
                plain: [
                    updateImages: [
                        [
                            filename: "deployment.yaml", // the files need to be in the gitopsConfig.deployments.sourcePath folder and in each of their own namespace folder e.g. k8s/staging/deployment.yaml
                            containerName: 'application',
                            imageName: 'imageName'
                        ]
                    ]
                ]
        ]
]
```

In plain pipelines, the library creates the deployment resources by updating the image tag within the deployment.

---

### Helm deployment
Besides plain k8s resources you can also use helm charts to generate the resources. You can choose between two types of
helm-repository-types. First of all there is the `repoType: HELM`, which is used to load tgz from helm-repositories.

```groovy
def gitopsConfig = [
        deployments: [
                helm: [
                    repoType: 'HELM',
                    repoUrl: 'https://charts.bitnami.com/bitnami',
                    credentialsId: 'creds',
                    version: '7.1.6',
                    chartName: 'nginx',
                    updateValues: [[fieldPath: "image.name", newValue: imageName]]
                ]
        ]
]
```

Then there is `repoType: GIT` - which can be used to load charts from a specific Git-Repository.

```groovy
def gitopsConfig = [
        deployments: [
                helm: [
                    repoType: 'GIT',
                    repoUrl: "https://git-repo/namespace/name",
                    credentialsId: 'creds',
                    version: '1.2.3', // tag, commit or branch
                    chartPath: 'chart', // defaults to empty string meaning root directory of repo
                    updateValues: [[fieldPath: "image.name", newValue: imageName]]
                ]
        ]
]
```

#### Conventions for helm deployment
- Application name is used as the release-name in Flux (not for argo, argo creates plain resources using `helm template`)
  > Helm Release name = `${application}`
- Extra k8s resources can also be deployed if you choose to have a Helm deployment
  > extraResources get copied into the gitops folder without being changed - can be used e.g. sealed-secrets
  > extraResources are always relative to the `${gitopsConfig.deployments.sourcePath}/${stage}`, you can specify files or directories

  e.g. in stage production =>

  > copies '/k8s/production/configMap.yaml' and '/k8s/production/secrets' to the gitops folder where every k8s resource in ./secrets will be copied to the gitops folder
---

## Validators

The library itself provides three validators `yamllint`, `kubeval` and `helmKubeval` to validate the generated resources.
You can disable the built-in operators and/or add your own.
The operators are processed sequentially in no particular order.

Example: Disable all built-ins and add a custom validator.

```groovy
node {
    stage('Deploy') {

        def gitopsConfig = [
            // ...
            validators        : [
                yamllint: [
                    enabled  : false,
                ],
                kubeval: [ 
                    enabled  : false,
                ],
                myVali: [
                    validator: new MyValidator(this),
                    enabled  : true,
                    config   : [
                        some: 'thing'
                    ]
                ]
            ]
        ]

        deployViaGitops(gitopsConfig)
    }
}

// Simple example that works with dynamic library loading, i.e. the library() step
class MyValidator {
    def script
    MyValidator(def script) {
        this.script = script
    }

    void validate(boolean enabled, String targetDirectory, Map config) {
        script.echo "Enabled: $enabled; targetDirectory: $targetDirectory; config: $config"
    }
}
```

### Custom validators

In general a custom validator must provide this method: `validate(boolean enabled, String targetDirectory, Map config)`

The library also offers a convenient base class [`com.cloudogu.gitops.gitopsbuildlib.Validator`](src/com/cloudogu/gitopsbuildlib/Validator.groovy).
However, this seems impossible to use with neither dynamic library loading via the `library()` nor with `@Library`,
because the library is loaded after the class is evaluated.

---

## Extra Files

If extra files are needed and are not k8s resources there is the `fileConfigMaps` property.

```groovy
def gitopsConfig = [
        fileConfigmaps: [ 
                [
                    name : "index",
                    sourceFilePath : "../index.html", // relative to deployments.sourcePath
                    stage: ["staging", "production"] // these have to match the `gitopsConfig.stages` 
                ]
        ]
]
```

This will generate a k8s ConfigMap with the content of this file as data:

```bash
Name:         index
Namespace:    fluxv1-staging                                                                                                                                                                                                                │
Data
====
index.html:
----
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes">
    <title>k8s-gitops-playground</title>
</head>
<body>
<h2>Hello cloudogu gitops playground!</h2>
</body>
</html>
```
