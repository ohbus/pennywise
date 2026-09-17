# OBS-01: Implement cross-cutting structured logging, MDC correlation, and observability tools

- Phase: quality
- Owner role: coordinator
- Dependencies: FND-08
- Owned paths: `libs/errors/`, `libs/observability/`, `docs/tasks/details/OBS-01.md`

## Outcome

Implement cross-cutting correlation and structured logging infrastructure across `libs/errors` and `libs/observability`.

## Requirements

1. In `libs/errors`:
   - Extract `RequestIdContext` from `RequestIdFilter.kt` into dedicated `RequestIdContext.kt` file.
   - Populate `org.slf4j.MDC` with key `"requestId"` in `RequestIdContext` / `RequestIdFilter`, ensuring reliable MDC cleanup in a `finally` block.
   - Add SLF4J logger to `RequestIdFilter` with `DEBUG` logs on request entry and exit (including duration in ms and response status code).
   - Add SLF4J logger to `GlobalErrorHandler`:
     - Log unhandled `Exception` at `ERROR` level with stack trace and `requestId`.
     - Log `MethodArgumentNotValidException`, `HttpMessageNotReadableException`, and 4xx `ResponseStatusException` at `WARN` level with field violation summaries (without echoing raw values or passwords).
     - Add explicit exception handler for `OptimisticLockingFailureException` logging at `WARN` level and returning HTTP 409 `CONFLICT`.
2. In `libs/observability`:
   - Implement `LoggingContext` for scoped background worker execution with automatic MDC cleanup.
   - Implement `SensitiveDataSanitizer` utility for email masking (`u***@domain.com`) and token redaction (`Bearer [REDACTED]`).
3. Maintain zero compiler warnings or regressions across all test suites.

## Acceptance criteria

- `RequestIdContext` resides in its own dedicated source file.
- `requestId` is present in `org.slf4j.MDC` during servlet request processing.
- `GlobalErrorHandler` logs unhandled exceptions at `ERROR` and validation/client errors at `WARN`.
- `./gradlew test --rerun-tasks --no-daemon` passes cleanly.
- `git diff --check` passes cleanly.

## Current limitation

Focused and full-suite tests pass, but compilation still reports pre-existing
GraphQL deprecation warnings and a warning in a separate uncommitted
ExpenseValidator workstream. Keep this task open until warning ownership and
cleanup are resolved.

## Validation commands

- `./gradlew :libs:errors:test :libs:observability:test --rerun-tasks --no-daemon`
- `./gradlew test --rerun-tasks --no-daemon`
- `python3 tools/contracts/validate.py`
- `git diff --check`
