# FND-03: Configure CI and executable quality checks

- Phase: scaffold
- Owner role: quality
- Dependencies: FND-01
- Owned paths: `tools/`, `.github/`

## Outcome

Add portable contract checks, CI and architecture verification.

## Implementation requirements

Contract lint, GraphQL validation, examples, dependency boundaries, build/tests and scan entrypoints are locally runnable.

## Acceptance criteria

CI wraps the same commands; record actual executed checks and external-download blockers honestly.

## Verification and handoff

Record exact commands, exit status, artifact paths, findings and unresolved blockers
in the task registry. No task is done until its deliverable is reviewed. Use the
approved contracts as the behavioral authority; update the specification and
register child tasks before expanding scope. Future implementation tasks are not
authorized to bypass the documentation gate or the current scaffold milestone.

