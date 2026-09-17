# OPS-04 — GitHub Actions CI and GHCR delivery

## Outcome

Provide separate pull-request, non-main push, and main push entry points. They
delegate to one reusable workflow so build, test, validation, and container
policy remain consistent while inputs control scope.

## Scope

The module matrix covers all four deployable applications and five libraries.
Main builds publish immutable SHA and branch tags to GHCR using `GITHUB_TOKEN`.
Pull requests and feature branches never receive package-write permissions.

## Verification

Parse every workflow, run the existing Gradle and contract checks, and inspect
the diff for whitespace errors. Hosted execution remains the responsibility of
GitHub after merge.

## Known limitation

The repository ktlint gate is tracked separately in DOC-12 and remains blocked
by verified incompatibility with Kotlin 2.4.20. CI runs the available Gradle
quality checks and reports that blocker rather than hiding it.
