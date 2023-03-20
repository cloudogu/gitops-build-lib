# gitops-build-lib

Jenkins pipeline shared library for automating deployments via GitOps.   
Take a look at [cloudogu/k8s-gitops-playground](https://github.com/cloudogu/k8s-gitops-playground) to see a fully 
working example bundled with the complete infrastructure for a gitops deep dive.  
See also our [blog post](https://cloudogu.com/en/blog/ciops-vs-gitops_en) that describes the challenges leading to this
library and some of the features it offers. 

If you have any questions, remarks or ideas regarding the `gitops-build-lib`, feel free to visit our [community](https://community.cloudogu.com/t/introducing-the-gitops-build-lib/108).  
Or if you want to chat with us about gitops in general, visit us [here](https://community.cloudogu.com/c/gitops-by-cloudogu/23)

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->

- [Use Case](#use-case)
- [Features](#features)
- [Examples](#examples)
  - [Minimal](#minimal)
  - [More options](#more-options)
  - [Real life examples](#real-life-examples)
- [Usage](#usage)
  - [Jenkins](#jenkins)
  - [GitOps tool](#gitops-tool)
    - [Flux v1](#flux-v1)
    - [ArgoCD](#argocd)
- [Default Folder Structure in source repository](#default-folder-structure-in-source-repository)
  - [Plain-k8s](#plain-k8s)
  - [Helm](#helm)
- [GitOps-Config](#gitops-config)
- [Stages](#stages)
  - [Namespaces](#namespaces)
  - [Conventions for stages](#conventions-for-stages)
- [Deployment](#deployment)
  - [Plain k8s deployment](#plain-k8s-deployment)
  - [Helm deployment](#helm-deployment)
    - [Conventions for helm deployment](#conventions-for-helm-deployment)
    - [`helm template` with ArgoCD application](#helm-template-with-argocd-application)
- [Folder Structure in destination gitops repository](#folder-structure-in-destination-gitops-repository)
- [Extra Files](#extra-files)
- [SCM-Provider](#scm-provider)
- [Validators](#validators)
  - [Custom validators](#custom-validators)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Use Case

Use Case realised by this library:

![](https://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/cloudogu/k8s-diagrams/master/diagrams/gitops-with-app-repo.puml&fmt=svg)

* Separation of app repo and GitOps repo
* Infrastructure as Code is maintained  in app repo,
* CI Server writes to GitOps repo and creates PullRequests.
* Lots of convenience features
* Same Pipeline API for different GitOps tools

## Features

* Write Kubernetes resources to a git repo (for a GitOps operator to pick them up) and creates PRs.
* Opinionated conventions for [folder structures](#default-folder-structure) and workflows.
* Support for [multiple stages](#stages) within the same gitOps Repo
    * Push to application branch and create PR (production) or
    * Push to main branch (e.g. "main") directly for staging deployment
* Support for different [GitOps tools/operators/vendors](#gitops-tool)
    * ArgoCD
    * Flux v1
* Deployment methods
    * [Plain Kubernetes resources](#plain-k8s-deployment) - write image tag into kubernetes deployments dynamically
        * add files to deployment which will be generated to a configmap
    * [Helm deployments](#helm-deployment) - set values (e.g. image tag) dynamically
        * add custom k8s resources
        * Support for different Helm-Repository types:  Helm, Git
* [Convert files into Kubernetes config maps](#extra-files). 
  This way you can edit them using full highlighting and linting support of your IDE.
* Fail early: [Validate](#validators) YAML syntax (yamllint) and against Kubernetes schema (kubeval) as well as Helm charts (helmKubeval)
* [Pluggable validators](#custom-validators): Simply add your own validators
* Extensible SCM providers
    * SCM-Manager
    * Other can easily be added due to an abstraction layer 

---

## Examples

The following examples range from simple to real-life and should help you get started.

Detailed instructions about the attributes can be found [here](#gitops-config).

### Minimal

This will simply [validate](#validators) and deploy the resources from the source repo's `k8s` folder (see 
[folder structure](#default-folder-structure) into the `gitops` repo, implementing two stages (see [stages](#stages)) 
using SCM-Manager (see [SCM provider](#scm-provider)).

```groovy
def gitopsConfig = [
    scm: [
        provider:       'SCMManager',
        credentialsId:  'scmm-user',
        baseUrl:        'http://scmm-scm-manager/scm',
        repositoryUrl:  'gitops'
    ],
    application: 'spring-petclinic',
    gitopsTool: 'FLUX' /* or 'ARGO' */,
    stages: [
        staging: [
            namespace: 'staging',
            deployDirectly: true
        ],
        production: [
            namespace: 'production',
            deployDirectly: false
        ]
    ],
    deployments: [
        plain     : []
        ]
    ]
]

deployViaGitops(gitopsConfig)
```

### More options

The following is an example shows all options of a **gitops-config** for a helm-deployment of an application. 
This would lead to a deployment of your staging environment by updating the resources of "staging" folder within your 
gitops-folder in git. For production it will open a PR with the changes.

```groovy
def gitopsConfig = [
    k8sVersion: '1.24.8', /* Default: '1.24.8' */
    scm: [
        provider:       'SCMManager',
        credentialsId:  'scmm-user',
        baseUrl:        'http://scmm-scm-manager/scm',
        repositoryUrl:  'gitops'
    ],
    cesBuildLibRepo: <cesBuildLibRepo> /* Default: 'https://github.com/cloudogu/ces-build-lib' */ ,
    cesBuildLibVersion: <cesBuildLibVersion> /* Default: a recent cesBuildLibVersion see deployViaGitops.groovy */ ,
    cesBuildLibCredentialsId: <cesBuildLibCredentialsId> /* Default: '', empty due to default public github repo */,
    application: 'spring-petclinic',
    gitopsTool: 'FLUX' /* or 'ARGO' */
    mainBranch: 'master' /* Default: 'main' */, 
    deployments: [
        sourcePath: 'k8s' /* Default: 'k8s' */,
        destinationRootPath: '.' /* Default: '.' */,
        /* See docs for helm or plain k8s deployment options */
        helm : [
            repoType : 'HELM',
            credentialsId : 'creds',
            mainBranch : 'main', /* Default: 'main' */, 
            repoUrl  : <helmChartRepository>,
            chartName: <helmChartName>,
            version  : <helmChartVersion>,
            updateValues  : [[fieldPath: "image.name", newValue: imageName]]
        ]
    ],
    folderStructureStrategy: 'GLOBAL_ENV', /* or ENV_PER_APP */
    stages: [
        staging: [ 
            namespace: 'my-staging',
            deployDirectly: true
        ],
        production: [
            namespace: 'my-production',
            deployDirectly: false 
        ],
    ],
    validators        : [
        /* Enalbed by default. Example on how to deactivate one.
         * See docs for more info on validators  */
        yamllint: [
            enabled  : false
        ]
]

deployViaGitops(gitopsConfig)
```

### Real life examples

**FluxV1:**
* [using petclinic  with helm and extra k8s-resources and extra files](https://github.com/cloudogu/k8s-gitops-playground/blob/main/applications/petclinic/fluxv1/helm/Jenkinsfile)
* [using petclinic with plain-k8s and extra files](https://github.com/cloudogu/k8s-gitops-playground/blob/main/applications/petclinic/fluxv1/plain-k8s/Jenkinsfile)
* [using nginx with helm and extra files](https://github.com/cloudogu/k8s-gitops-playground/blob/main/applications/nginx/fluxv1/Jenkinsfile)

**ArgoCD:**
* [using petclinic with helm and extra k8s-resources and extra files](https://github.com/cloudogu/k8s-gitops-playground/blob/main/applications/petclinic/argocd/helm/Jenkinsfile)
* [using petclinic with plain-k8s](https://github.com/cloudogu/k8s-gitops-playground/blob/main/applications/petclinic/argocd/plain-k8s/Jenkinsfile)
* [using nginx with helm and extra files](https://github.com/cloudogu/k8s-gitops-playground/blob/main/applications/nginx/argocd/Jenkinsfile)

---

## Usage

Two parts need completion in order to get up and running with the library:
* Trigger the library in your build, which writes infrastructure code into your GitOps repo.
* Set up the GitOps tool, so it deploys the infrastructure code from your GitOps repo.

### Jenkins

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
@Library('github.com/cloudogu/gitops-build-lib@0.0.8')
import com.cloudogu.gitops.gitopsbuildlib.*
```

To utilize the library itself, there is little to do:
- Setup a [`gitopsConfig`](#examples) containing the following sections
    - properties (e.g. remote urls to scm, application, GitOps tool, etc.)
    - stages
    - deployments
    - validators
    - fileConfigMaps
- Call `deployViaGitops(gitopsConfig)`

### GitOps tool

Having the library write the configuration into your GitOps repository is the first part.
Next, make your GitOps tool deploy it. Obviously this setup depends on the GitOps tool used.

The library hides some implementation specifics of the individual tools. For example, for Flux, it creates `HelmRelease` CRs, for argo it calls `helm template`, see [Helm](#helm-deployment).

#### Flux v1

You can make this library work with Flux v1 with the following steps:

- Create a repository in your SCM for the payload of your applications
- Configure the Flux operator to use this repository
- If you want to use Helm applications you will need a Helm operator because this library is creating `HelmRelease` CRs

See [Example for operator configurations in GitOps Playground](https://github.com/cloudogu/k8s-gitops-playground/tree/main/fluxv1)  
See [Example application with Flux v1](https://github.com/cloudogu/k8s-gitops-playground/tree/main/applications/petclinic/fluxv1/helm)

#### ArgoCD

You can make this library work with your ArgoCD with the following steps:

- Create a repository in your SCM for the payload of your applications
- Add this repository to your ArgoCD configuration
- Add application definitions for the different stages of your application e.g. (your staging application):
    ```yaml
    apiVersion: argoproj.io/v1alpha1
    kind: Application
    metadata:
      name: petclinic-staging
      namespace: argocd
    spec:
      destination:
        namespace: argocd-staging
        server: https://kubernetes.default.svc
      project: petclinic
      source:
        path: staging/spring-petclinic
        repoURL: http://scm/repo/gitops
        targetRevision: main
        directory:
          recurse: true
      syncPolicy:
        automated: {}
    ```
- Configure the payload repository through the `scm` key in your [gitopsconfig](#gitops-config)

See [Example of ArgoCD configuration in GitOps Playground](https://github.com/cloudogu/k8s-gitops-playground/tree/main/argocd)  
See [Example of ArgoCD application in GitOps Playground](https://github.com/cloudogu/k8s-gitops-playground/tree/main/applications/petclinic/argocd/helm)

---

## Default Folder Structure in source repository

A default project structure in your application repo could look like the examples below. Make sure you have your k8s 
and/or helm resources bundled in a folder. This specific resources folder (here `k8s`) will later be specified by the 
`sourcePath` within the deployments section of your `gitopsConfig`.

### Plain-k8s
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

### Helm
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

---

## GitOps-Config
The gitops config is basically a groovy map following conventions to have a descriptive way of defining deployments. 
You can find a complete yet simple example [here](#examples).

**Properties**

First of all there are some mandatory properties e.g. the information about your gitops repository, the application repository and the gitops tool to be used.

* `application: 'spring-petclinic'` - Name of the application. Used as a folder in GitOps repo
* `gitopsTool: 'ARGO'` - Name of the gitops tool. Currently supporting `'FLUX'` (for now only fluxV1) and `'ARGO'`.  
* and some optional parameters (below are the defaults) for the configuration of the dependency to the ces-build-lib or the default name for the git branch:
   * `cesBuildLibRepo:    'https://github.com/cloudogu/ces-build-lib'`
   * `cesBuildLibVersion: '1.62.0'`
   * `mainBranch:         'main'`
* Optional: `k8sVersion: '1.24.8'` Is used for target k8s version in helm template in Argo CD deployments with helm. Is also used to determine the kubectl version, when no specific buildImage is specified.  
  It is recommended to use a Jenkins environment variable to specify the version, so that you don't have to bump every pipeline after a k8s version upgrade in your cluster, e.g.
```
def gitopsConfig = [  
    k8sVersion: env.K8S_VERSION_TEAM_A  
]
```

---

## Build Images
The GitOps-build-lib uses some docker images internally (To run Helm or Kubectl commands and specific Validators inside a docker container).  
All of these have set default images, but you can change them if you wish to.

```groovy
def gitopsConfig = [
    buildImages: [
        // These are used to run helm and kubectl commands in the core logic
        helm: 'ghcr.io/cloudogu/helm:3.11.1-2',
        // if you specify k8sVersion parameter, then by default bitnami/kubectl:${k8sVersion} will be used
        kubectl: 'bitnami/kubectl:1.24.8',
        // These are used for each specific validator via an imageRef property inside the validators config. See [Validators] for examples.
        kubeval: 'ghcr.io/cloudogu/helm:3.11.1-2',
        helmKubeval: 'ghcr.io/cloudogu/helm:3.11.1-2',
        yamllint: 'cytopia/yamllint:1.25-0.9'
    ]
]
```

Optional - if image is in a private repository, you can pass a `credentialsId` for pulling images.

```groovy
def gitopsConfig = [
        buildImages: [
            helm: [ 
                image: 'ghcr.io/cloudogu/helm:3.11.1-2',
                credentialsId: 'myCredentials'
                ],
            // ...
        ]
]
```

## Stages
The GitOps-build-lib supports builds on multiple stages. A stage is defined by a name and contains a namespace (used to
generate the resources) and a deployment-flag:

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
it will create a PR on your `gitops-folder`. **Remember** there are important conventions regarding namespaces and the folder structure (see [namespaces](#namespaces)).

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

**Important note**

Under your `gitopsConfig.deployments.sourcePath` (here `k8s`) the subfolders have to be named exactly as the stages
under `gitopsConfig.stages`. The same applies to the `values-*` files if you have a Helm application.

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

For Helm:
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
            destinationRootPath: '.', // Root-Subfolder in the gitops repository, where the following folders for stages and apps shall be created. Default: '.'
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
                    repoUrl: "https://git-repo/namespace/name",  // git-repo must have a 'main' branch due to limitations of the git-step within jenkins
                    credentialsId: 'creds',
                    version: '1.2.3', // tag, commit or branch
                    chartPath: 'chart', // the path relative to root in the git repo. If the chart is at root level you can ommit this property
                    updateValues: [[fieldPath: "image.name", newValue: imageName]]
                ]
        ]
]
```

**Note:**

Due to limitations in the git-step of Jenkins, we have to clone from a specific branch rather then having the 
git client checkout the default branch given within the HEADs meta information. This specific branch is the
`main` branch. Make sure the git-repository has a main-branch, else the deployment step will fail. After a successful clone 
it checks out the given version as expected. 

#### Conventions for helm deployment
- Application name is used as the release-name in Flux (not for argo, argo creates plain resources using `helm template`)
  > Helm Release name = `${application}`
- Extra k8s resources can also be deployed if you choose to have a Helm deployment
  > extraResources get copied into the gitops folder without being changed - can be used e.g. sealed-secrets
  > extraResources are always relative to the `${gitopsConfig.deployments.sourcePath}/${stage}`, you can specify files or directories

  e.g. in stage production =>

  > copies '/k8s/production/configMap.yaml' and '/k8s/production/secrets' to the gitops folder where every k8s resource in ./secrets will be copied to the gitops folder

#### `helm template` with ArgoCD application

We decided to generate plain k8s Resources from Helm applications before we push it to the gitops Repository for the following reasons:

- With ArgoCD you can only set one source for your application. In case of helm it is common to have a source Repository for your chart and a scource Repository for your configuration files (values.yaml). In order to use two different sources for your helm application you will need some sort of workaround (e.g. Helm dependencies in `Chart.yaml`).  
- ArgoCD itself uses `helm template` to apply plain k8s Resources to the cluster. By templating the helm application before pushing to the gitops repository, we have the same resources in our repository as in our cluster. Which leads to increased transparency.
The parameter `k8sVersion` from the gitops config is used as a parameter with `--kube-version` in order to template version-specific manifests such as changed api versions.

---

## Folder Structure in destination gitops repository

You can customize in which path the final manifests of the application will be created in the gitops repository. For this, you can modify the following parameters:
```groovy
def gitopsConfig = [
    deployments: [
        destinationRootPath: '.' /* Default: '.' */
    ],
    folderStructureStrategy: 'GLOBAL_ENV' /* Default: 'GLOBAL_ENV', or ENV_PER_APP */
]
```
* `destinationRootPath`: Specifies in which subfolder the following folders of `folderStructureStrategy` are created. Defaults to the root of the repository.
* `folderStructureStrategy`: Possible values: 
  * `GLOBAL_ENV`: The manifests will be commited into `$DESTINATION_ROOT_PATH/STAGE_NAME/APP_NAME/` in the destination gitops repository
  * `ENV_PER_APP`: The manifests will be commited into `$DESTINATION_ROOT_PATH/APP_NAME/STAGE_NAME/` in the destination gitops repository


Example for **Global Environments** vs **Environment per App** ([Source](https://github.com/cloudogu/gitops-patterns#implementing-release-promotion)):

  ![Global Envs](https://github.com/cloudogu/gitops-talks/blob/1744c1d/images/global-environments.svg)
  ![Env per app](https://github.com/cloudogu/gitops-talks/blob/1744c1d/images/environment-per-app.svg)

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
Namespace:    staging                                                                                                                                                                                                                │
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

## SCM-Provider

The scm-section defines where your 'gitops-repository' resides (url, provider) and how to access it (credentials):

```groovy
def gitopsConfig = [
    ...
    scm: [
        provider:       'SCMManager',   // This is the name of the scm-provider in use, for a list of supported providers watch below!
        credentialsId:  'scmm-user',    // ID of credentials defined in Jenkins used to authenticated with SCMM
        baseUrl:        'http://scmm-scm-manager/scm',  // this is your gitops base url 
        repositoryUrl:  'fluxv1/gitops' // this is the gitops repo
    ],
    ...
]
```
It currently supports the following scm-provider:

- [SCMManager](https://www.scm-manager.org/)

To empower people to participate and encourage the integration of other scm-software (e.g. github), we decided to implement an abstraction for the
scm-provider. By extending the SCM-Provider class you can integrate your own provider! Please feel free to contribute!

Example:

```groovy
import com.cloudogu.gitopsbuildlib.scm.SCMProvider

class GitHub extends SCMProvider {
    @Override
    void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl }

    @Override
    void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl }

    @Override
    void setCredentials(String credentialsId) { this.credentials = credentialsId }

    @Override
    String getRepositoryUrl() { return "${this.baseUrl}/${repositoryUrl}"}

    @Override
    void createOrUpdatePullRequest(String stageBranch, String mainBranch, String title, String description) {
        // TODO: this is a specific implementation for github
        // 1. creating a pr on the given repo with the given details
        // 2. update a pr on the given repo if it already exists
        //
        // Note: Credentials given are credentialsId from Jenkins!
    }
}
```

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
```

Each Validator has a property called `imageRef` and `image` inside the `config` property.  
`imageRef`'s value defaults to the key in the `buildImages` property and will replace the key-name with the actual image corresponding to the value.

```groovy
def gitopsConfig = [
        validators: [
            kubeval: [
                enabled: true,
                config: [
                    imageRef: 'kubeval' // this corresponds to the key/value pair in buildImages which internally will become imageRef: 'ghcr.io/cloudogu/helm:3.5.4-1'
                ]
            ]
        ]
    ]
```

If you wish to change the image, you can do so by changing the image in the corresponding key/value pair in `buildImages` or by setting the image directly via the `image` property.
First, the library will check if the `image` property is set and if so, will use its value as the image.

```groovy
def gitopsConfig = [
        validators: [
            kubeval: [
                enabled: true,
                config: [
                    image: 'ghcr.io/cloudogu/helm:3.5.4-1'
                ]
            ]
        ]
    ]
```

If the `image` value is not set, it resorts to the `imageRef` property.

### Custom validators

The library offers a convenient base class [`com.cloudogu.gitops.gitopsbuildlib.Validator`](src/com/cloudogu/gitopsbuildlib/Validator.groovy).
However, this seems impossible to use with neither dynamic library loading via the `library()` nor with `@Library`,
because the library is loaded after the class is evaluated.  
If you need to use `library()` or `@Library` then your validator needs to implement the following three methods:

- `void validate(boolean enabled, String targetDirectory, Map config)`
- `SourceType[] getSupportedSourceTypes()`
- `GitopsTool[] getSupportedGitopsTools()`

```groovy
import com.cloudogu.gitopsbuildlib.deployment.GitopsTool
import com.cloudogu.gitopsbuildlib.deployment.SourceType

class MyValidator extends Validator {

    MyValidator(def script) {
        super(script)
    }

    @Override
    void validate(boolean enabled, String targetDirectory, Map validatorConfig, Map gitopsConfig) {
        script.echo "Enabled: $enabled; targetDirectory: $targetDirectory; validatorConfig: $validatorConfig; gitopsConfig: $gitopsConfig"
    }

    @Override
    SourceType[] getSupportedSourceTypes() {
        return [SourceType.HELM, SourceType.PLAIN]
    }

    @Override
    GitopsTool[] getSupportedGitopsTools() {
        return [GitopsTool.ARGO, GitopsTool.FLUX]
    }
}
```

In general a custom validator may implement the Validator class. You therefore have to implement the following methods:

`void validate(boolean enabled, String targetDirectory, Map config)`
- Here lies your validation code or the entrypoint for more complex validation processes

`SourceType[] getSupportedSourceTypes()`  
- This method returns a collection of supported Source Types.  
The SourceType determines which resources are going to be validated.
There are two locations where resources can be validated.   
They are differentiated by the resource-type of which there are two right now.
    - Helm resources
    - Plain k8s resources
    

**Visual representation of the folder structure on the Jenkins agent**
```
├── jenkinsworkdir/
   └── .configRepoTempDir/
      └── ${stage}/
         └── ${application}/
            ├── extraResources/
            ├── generatedResources/
            ├── deployment.yaml
            └── ...
   └── .helmChartTempDir/
      └── chart/
         └── ${chartPath}/ (for git repo) or ${chartName}/ (for helm repo)
      └── mergedValues.yaml
```

**Helm resources** - `.helmChartTempDir`:  
This location is only temporary and is being used for the helm chart to be downloaded and the mergedValues.file (values-shared.yaml + values-${stage}.yaml)
Only Validators which support Helm schemas should operate on this folder

**Plain k8s resources** - `.configRepoTempDir`:  
This location is for your plain k8s resources. This folder is also the gitops folder which will be pushed to the scm.
It contains your k8s resources in the root and two extra folders for additional k8s resources:
`extraResources`: Only needed for a Helm deployment if you whish to deploy plain k8s resources in addition to the helm deployment. See: [Important note in Namespaces](#namespaces)  
`generatedResources`: If you have files which need to be deployed as a configMap. See: [Extra Files](#extra-files)

`GitopsTool[] getSupportedGitopsTools()`  
This determins on which GitopsTool the validator will run. We implemented this feature since Argo already uses `helm template` and `kubeval` internally so we don't need `helm kubeval` since it does exactly the same.
So we defined `HelmKubeval` as only needed to be executed on a `FLUX` operator.
