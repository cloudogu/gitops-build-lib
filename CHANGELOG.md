# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]


## [0.3.0](https://github.com/cloudogu/gitops-build-lib/releases/tag/0.3.0) - 2023-XX-YY

### Added
- Specify version for `helm template` through the `k8sVersion` parameter

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
