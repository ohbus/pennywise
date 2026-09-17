# FND-01: Create verified Gradle scaffold and UI placeholder

- Phase: scaffold
- Owner role: coordinator
- Dependencies: DOC-08
- Owned paths: `app/`, `libs/`, `build-logic/`, `gradle/`, `settings.gradle.kts`, `build.gradle.kts`

## Outcome

Create Gradle multi-project scaffold after DOC-08 passes.

## Implementation requirements

Apps: accounts, expense-core, notifications, bff. Technical libs: db, security, observability, test-support. Reserve app/web/README.md.

## Acceptance criteria

Use stable verified compatible versions, aligned Java/Kotlin targets and wrapper; root checks and independent app packaging pass; no business endpoints are invented.

## Verification and handoff

Record exact commands, exit status, artifact paths, findings and unresolved blockers
in the task registry. No task is done until its deliverable is reviewed. Use the
approved contracts as the behavioral authority; update the specification and
register child tasks before expanding scope. Future implementation tasks are not
authorized to bypass the documentation gate or the current scaffold milestone.

