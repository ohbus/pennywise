# FND-04: Repository workflow Makefile

- Phase: foundation
- Owner: coordinator
- Dependencies: FND-01, FND-03
- Owned paths: `Makefile`, `README.md`, `docs/operations/quickstart.md`

## Outcome

Provide one discoverable command surface for tool checks, contract validation,
tests, coverage, packaging, Compose lifecycle, image builds, production config,
smoke E2E checks, cleanup, and Git status.

## Acceptance

Targets call existing scripts and Gradle tasks, fail on missing prerequisites,
do not embed credentials, and are documented with expected product-scaffold
limitations. `make check`, `make e2e`, and Compose validation must pass.
