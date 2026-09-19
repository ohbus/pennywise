# Continuous integration and delivery

Three thin workflows select policy: `ci-pr.yml` validates pull requests,
`ci-branch.yml` validates non-master pushes, and `ci-master.yml` validates master and
publishes four application images. Verification, checks, and E2E execution live
in `_reusable-ci.yml`, while container image delivery lives in `ci-master.yml` so
feature branch and pull request workflows can operate with read-only permissions
without encountering GitHub Actions reusable workflow permission validation errors.

The reusable workflow applies Gradle dependency and build caching with
content-addressed keys and restore fallbacks. E2E uses the same policy, while
Docker BuildKit layers use the GitHub Actions cache backend. Cache misses only
reduce speed and never change verification behavior.
Workflows declare `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24: 'true'` in their top-level
`env` blocks, ensuring all runner steps and composite JavaScript actions run on
Node 24 ahead of runner deprecation deadlines.

Verification and E2E jobs each receive isolated PostgreSQL 17 and RabbitMQ
4.3 services. Docker health checks (`pg_isready` and `rabbitmq-diagnostics ping`)
must pass before job steps begin; no service state is shared between matrix jobs.

The matrix tests every application and library in parallel after a single
preflight, validates contracts, REST path structure, GraphQL schema/resolver
parity, and Compose files, runs Gradle `test`, `check`, and JaCoCo, and builds
application jars. The lightweight checks also run the acceptance unit suite,
workflow YAML parsing, strict Python typing via `uvx`/mypy, and
`git diff --check`. Jobs use Microsoft Build of OpenJDK. Local Python tooling
must use `uv` or `uvx` rather than installing packages into the system
interpreter.
Every test run publishes a readable test summary directly to GitHub Actions job
summaries (`test-summary/action@v2`) and uploads JUnit XML and HTML reports as
job artifacts with `if: always()` retention.
For application projects, `_reusable-ci.yml` uploads the built executable
`bootJar` artifact (`app-jar-<service>`). Master image publishing in `ci-master.yml`
downloads this pre-built artifact and packages the runtime image with
`infra/docker/Dockerfile.fast` (`eclipse-temurin:25-jre`), eliminating redundant
JVM compilation inside Docker.
Master image jobs create an explicit `docker-container` Buildx builder before
using the GitHub Actions cache backend; each service matrix entry has its own
cache scope. They publish SHA and branch tags to
`ghcr.io/<owner>/pennywise-<service>` with the job-scoped `GITHUB_TOKEN`.

Run the complete hosted verification equivalent locally with:

```sh
make ci
```

This runs the same contract and Compose preflight, then asks Gradle to execute
tests, checks, JaCoCo reporting, and application packaging with `--parallel`.
Gradle's project task graph avoids rebuilding work that is already up to date.
Run `make ci-e2e` when the E2E smoke stage should be included. The smaller
targets remain useful when iterating: `make contracts`, `make compose-config`,
`make test`, `make coverage`, and `make package`.

Hosted verification intentionally fans out the nine module checks across
separate runners; local execution shares one checkout and JVM, so Gradle
parallelism is the corresponding local optimization. Hosted master image builds
fan out only after every verification matrix job succeeds. Local image builds
are available independently through `make docker-build-all` or
`make docker-build-<service>` and never push to a registry.

Workflow files are parsed with Ruby's YAML parser as part of CI maintenance.
The reusable workflow is the single source for action versions, Microsoft
OpenJDK setup, and verification matrix membership; `ci-master.yml` owns GHCR
delivery policy once verification succeeds. Trigger workflows only select event
policy and inputs. Kotlin ktlint remains tracked as DOC-12
because its verified toolchain is not compatible with the current Kotlin
baseline.

Use `make workflow-validate` to parse all workflow files and `make acceptance`
to produce `build/reports/acceptance/qa-01.json`.
