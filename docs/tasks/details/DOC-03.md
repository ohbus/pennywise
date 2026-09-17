# DOC-03: Specify technology and project architecture

- Phase: documentation
- Owner role: architecture
- Dependencies: DOC-02
- Owned paths: `docs/implementation/technology-decisions.md`, `docs/architecture/`

## Outcome

Document the complete technology rationale and target directory layout.

## Implementation requirements

Use JPA/Hibernate plus targeted SQL, Flyway, PostgreSQL, REST, GraphQL, RabbitMQ, and modular service boundaries.

## Acceptance criteria

Record exact candidate versions separately from verified versions; cover authentication, transaction ownership and deferred choices.

## Verification and handoff

Record exact commands, exit status, artifact paths, findings and unresolved blockers
in the task registry. No task is done until its deliverable is reviewed. Use the
approved contracts as the behavioral authority; update the specification and
register child tasks before expanding scope. Future implementation tasks are not
authorized to bypass the documentation gate or the current scaffold milestone.

