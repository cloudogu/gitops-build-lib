# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.7.0](https://github.com/cloudogu/gitops-build-lib/releases/tag/0.7.0) - 2024-10-14

### Added
- Add additional docker runs args support
- Add welcome message and print configs at application start

### Changed
- Disable yamllint image due to image is not longer maintained 
- Update k8s, helm and yamllint

## [0.6.0](https://github.com/cloudogu/gitops-build-lib/releases/tag/0.6.0) - 2024-08-26

### Changed
- Change License to agpl3.0

## [0.5.0](https://github.com/cloudogu/gitops-build-lib/releases/tag/0.5.0) - 2023-05-03

### Added
- Make `deployments.plain` work with `CronJob` 

## [0.4.0](https://github.com/cloudogu/gitops-build-lib/releases/tag/0.4.0) - 2023-03-21

### Added

- Helm Repo Type `LOCAL`

## [0.3.0](https://github.com/cloudogu/gitops-build-lib/releases/tag/0.3.0) - 2023-03-21

### Added
- Add `k8sVersion` parameter.
  - Specifies `--kube-version ` for `helm template` (ArgoCD) and
  - `kubectl` image (if no explicit `buildImages.kubectl` parameter is used)
  - ⚠️ `k8sVersion` has a default value, so this is no breaking change per se. However, depending on the helm chart used, 
    the rendered result might look different from before where no `--kube-version` parameter was used.  
    We recommend setting the `k8sVersion`. Double-check the commits in your GitOps repo. 

### Changed
- Changed kubectl image from `lachlanevenson/k8s-kubectl` to `bitnami/kubectl`, because it is available for every k8s version out there

### Fixed
- Don't fail when no `values-shared.yaml` is provided

## [0.2.0](https://github.com/cloudogu/gitops-build-lib/releases/tag/0.2.0) - 2023-03-10

### Added
- Support different base path in destination gitops repository with `destinationRootPath` #26
- Support different folder strategies with `folderStructureStrategy` #26
- Optional `credentialsId` for build images #19
- Add option for other mainbranches in helm git repositories #19

### Changed

- Bump default cesBuildLib version to 1.62.0 #26
- Bump default kubectl image to 'lachlanevenson/k8s-kubectl:v1.24.8' #26
- Bump default helm image to 'ghcr.io/cloudogu/helm:3.11.1-2' #26

### Removed

- Disable kubeval and helm kubeval in default config, because they are deprecated (we will introduce another linting tool later) #26

### Fixed
- Add namespace to argo helm release #19

## 0.0.1 - 0.1.3

No change log provided. See GitHub release pages for details, e.g.
https://github.com/cloudogu/gitops-build-lib/releases/tag/0.1.3
