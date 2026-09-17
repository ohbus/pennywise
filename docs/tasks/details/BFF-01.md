# BFF-01: Implement GraphQL adapters

- Phase: future_implementation
- Owner role: bff
- Dependencies: FND-02
- Owned paths: `app/bff/`

## Outcome

Implement database-free GraphQL-to-REST adapters.

## Implementation requirements

Use WebFlux/WebClient, bounded fanout, authenticated identity, deadlines and stable idempotency forwarding; no blocking JDBC.

## Acceptance criteria

Schema conformance, nullability/partial failures and error mapping tests pass; REST resource owners enforce authorization independently.

## Verification and handoff

Record exact commands, exit status, artifact paths, findings and unresolved blockers
in the task registry. No task is done until its deliverable is reviewed. Use the
approved contracts as the behavioral authority; update the specification and
register child tasks before expanding scope. Future implementation tasks are not
authorized to bypass the documentation gate or the current scaffold milestone.

## Current increment

Added a nonblocking WebClient gateway for Expense Core group create/list calls,
with configurable service URL and bearer-token forwarding. GraphQL resolver
registration, bounded fanout, deadline policy, REST problem-to-GraphQL error
mapping, and end-to-end adapter tests remain pending.

The current increment adds GraphQL `groups` query and `createGroup` mutation
entry points that delegate to the gateway; schema registration and transport
integration remain pending until the BFF schema resource is wired.

The schema is now copied from `contracts/graphql/schema.graphqls` during the BFF
resource build, with GraphQL HTTP/WebSocket path configuration. Resolver coverage
for the remaining operations and transport-level tests remain pending.
