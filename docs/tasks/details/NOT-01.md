# NOT-01: Implement inbox and notification delivery

- Phase: future_implementation
- Owner role: notifications
- Dependencies: MSG-01, ACC-01
- Owned paths: `app/notifications/`

## Outcome

Implement notification inbox/preferences and external delivery.

## Implementation requirements

Apply event-ID deduplication, recipient authorization, rate limits, durable delivery jobs and stable provider keys where supported.

## Acceptance criteria

Notification failures never roll back finances; duplicate deliveries do not duplicate inbox records; ambiguous email outcomes remain documented.

## Verification and handoff

Record exact commands, exit status, artifact paths, findings and unresolved blockers
in the task registry. No task is done until its deliverable is reviewed. Use the
approved contracts as the behavioral authority; update the specification and
register child tasks before expanding scope. Future implementation tasks are not
authorized to bypass the documentation gate or the current scaffold milestone.

## Current increment

Added authenticated notification preference read/update endpoints with a
member-scoped adapter and controller coverage. Durable preferences, inbox REST
listing, rate limits, delivery jobs, and external provider integration remain
pending.

Added an authenticated member-scoped inbox read endpoint with newest-first
ordering and a bounded first page. Durable inbox storage, cursor pagination,
delivery jobs, rate limits, and provider integration remain pending.

Added preference-aware, deduplicated delivery decisions before provider dispatch.
Provider retries, rate limits, durable jobs, and ambiguous external outcomes
remain pending.

Added a per-recipient sliding-window rate limiter with deterministic clock
injection for tests. Distributed limits and durable counter storage remain
pending for production deployment.

Added deterministic retry classification with exponential backoff and parking
after the attempt limit. Durable job scheduling and provider-specific failure
classification remain pending.

Added bounded, URL-safe cursor pagination for inbox reads with a stable
timestamp and notification-ID sort key. Invalid cursors and out-of-range page
limits are rejected through the shared error flow. Preference reads and writes
now reject blank authenticated subjects. Verified with
`./gradlew :app:notifications:test --no-daemon` (exit 0).

Aligned package declarations across the Notifications module (`consumer`, `delivery`,
`inbox`, and `preferences`) with directory paths to match domain subpackage structure,
consistent with `NotificationPreferenceEntity`. Verified with `./gradlew :app:notifications:test --no-daemon` (exit 0).
