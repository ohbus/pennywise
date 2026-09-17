# FND-05: Shared REST error flow

- Phase: foundation
- Owner: coordinator
- Dependencies: FND-01, DOC-04
- Owned paths: `libs/errors/`, service build files, `contracts/rest/`, `docs/`

## Outcome

All REST services emit one `application/problem+json` envelope with a stable
machine-readable error code, HTTP status, request ID, safe detail, and optional
field violations. A request ID is generated at ingress or preserved from a valid
caller value and is returned in the response header and body.

## Acceptance

Validation, authentication, authorization, not-found, conflict, idempotency and
unexpected errors map consistently. Unexpected exception details are logged with
the request ID but never returned. Controller tests prove the same response shape
from the Expense Core service; the shared library is imported by every REST app.
