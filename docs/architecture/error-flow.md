# Consistent error flow

Every REST request passes through the service's request-ID filter and boundary
validation. The filter accepts a bounded alphanumeric `X-Request-Id` or creates
one, returns it on every response, and makes it available to logs and tracing.

All REST services import `libs/errors`. Its advice maps framework and domain
failures to `application/problem+json` with this shape:

```json
{
  "type": "https://pennywise.example/problems",
  "title": "Request validation failed",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "requestId": "req_123",
  "detail": "One or more fields are invalid",
  "timestamp": "2026-09-17T19:00:00Z",
  "violations": [{"field": "totalMinor", "message": "must match ..."}]
}
```

Clients branch on `code`, never free-text `detail`. The stable vocabulary is
`VALIDATION_FAILED`, `UNAUTHENTICATED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`,
`IDEMPOTENCY_CONFLICT`, `RATE_LIMITED`, and `INTERNAL_ERROR`. Unexpected errors
retain details only in correlated server logs. GraphQL maps the same code into
`errors[].extensions.code` and preserves the request ID.

Request validation is mandatory on every command/query DTO. Domain invariants
remain mandatory after transport validation; a valid JSON shape can still be an
invalid expense or unauthorized operation.
