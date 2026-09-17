# FND-06: Central dependency and plugin catalog

- Phase: foundation
- Owner: coordinator
- Dependencies: FND-01
- Owned paths: `gradle/libs.versions.toml`, `*.gradle.kts`, `infra/versions.env.example`

## Outcome

Every Gradle plugin and dependency version is defined once in the version
catalog. Projects use aliases or catalog version references and contain no
duplicated version literals. Docker image tags are centrally documented in a
non-secret operations version file.

## Acceptance

Dependency resolution and tests pass after migration. A repository search finds
no unapproved dependency version literals in project build files. Upgrading a
library requires one catalog change plus its verification evidence.
