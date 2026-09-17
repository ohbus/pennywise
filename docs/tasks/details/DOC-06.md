# DOC-06: Specify GraphQL and operation mapping

- Phase: documentation
- Owner role: contracts
- Dependencies: DOC-04, DOC-05
- Owned paths: `contracts/graphql/`

## Outcome

Author GraphQL SDL and explicit mappings to REST operations.

## Implementation requirements

Queries/mutations use HTTPS; group invalidations use authenticated WebSocket subscriptions; preserve stable domain error codes.

## Acceptance criteria

Schema builds; representative operations validate; money is a string-backed scalar; every field has an owner and pagination limits.

## Verification and handoff

Record exact commands, exit status, artifact paths, findings and unresolved blockers
in the task registry. No task is done until its deliverable is reviewed. Use the
approved contracts as the behavioral authority; update the specification and
register child tasks before expanding scope. Future implementation tasks are not
authorized to bypass the documentation gate or the current scaffold milestone.

