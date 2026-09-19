# Error catalog governance

This document governs the ERR-01 through ERR-12 migration. Client code branches
on `code`, never on `detail`. A code identifies one stable scenario and belongs
to exactly one service and feature area.

## Naming

Use `SERVICE_AREA_SCENARIO` in uppercase. Service prefixes are `ACC`, `EXP`,
`NOT`, and `BFF`. Examples are `EXP_GROUP_NOT_FOUND`,
`EXP_EXPENSE_INVALID_TOTAL`, and `BFF_UPSTREAM_ACCOUNTS_UNAVAILABLE`.

Do not create generic codes such as `BAD_REQUEST`, `DATABASE_ERROR`, or
`UNKNOWN_ERROR`. A generic unexpected code is permitted only at an unavoidable
boundary and must have an `errorId` plus a server-side stack trace.

## Required decision fields

Every catalog entry records its owner, component, operation, HTTP status, title,
safe detail, severity, retryability, and lifecycle. New codes require contract
validation, implementation tests, client mapping review, and a dashboard/alert
decision. Reuse an existing code only when the scenario, retry policy, and
client remediation are identical.

## Response and logging policy

REST problems contain `code`, `source`, `component`, `operation`, `requestId`,
`errorId`, status, timestamp, and safe detail. GraphQL copies these into
`errors[].extensions`. Logs contain the same identifiers and the sanitized cause.
Never return tokens, SQL, stack traces, raw amounts, or untrusted values.

Metrics use only bounded labels: service, component, operation, code, and
status. Request IDs and resource IDs are for logs/traces, not metric labels.

## Review checklist

- Is this a distinct client-remediation scenario?
- Is the service and feature prefix correct?
- Is HTTP status and retryability explicit?
- Are field/resource details safe?
- Is the catalog updated before code is merged?
- Are REST, GraphQL, Bruno, and integration tests present?
- Are logs correlated without high-cardinality metrics?
