# MSG-01: Implement outbox and broker delivery

- Phase: future_implementation
- Owner role: messaging
- Dependencies: FND-02
- Owned paths: `app/expense-core/`, `app/notifications/`

## Outcome

Implement transactional outbox relay and durable broker integration.

## Implementation requirements

Publish stable IDs with confirms/returns; recover leases; consumer inbox deduplicates effects; retry and parking preserve messages.

## Acceptance criteria

Failure tests cover broker down, lost confirm, crash after consumer commit, unavailable retry target and reordered events.

## Verification and handoff

Record exact commands, exit status, artifact paths, findings and unresolved blockers
in the task registry. No task is done until its deliverable is reviewed. Use the
approved contracts as the behavioral authority; update the specification and
register child tasks before expanding scope. Future implementation tasks are not
authorized to bypass the documentation gate or the current scaffold milestone.

## Current increment

Added a deterministic outbox relay contract slice with stable event IDs,
ordered leasing, attempt counts, lease recovery, and publish acknowledgement.
The persistent transactional table, RabbitMQ publisher confirms, inbox
deduplication, retry policy, and parking workflow remain pending.

Added a consumer-side inbox deduplication boundary in Notifications. It is an
in-memory contract adapter; transactional inbox persistence and broker consumer
integration remain pending.

Added explicit outbox rejection semantics: failed publishes are rescheduled at
an injected-clock availability time, while messages at or beyond the configured
attempt limit are parked and cannot be claimed again. This remains an in-memory
adapter pending transactional PostgreSQL persistence and RabbitMQ confirms.

Added a transport-neutral `BrokerPublisher`/`BrokerConsumer` boundary and an
in-memory adapter. The boundary models publisher confirms, stable event IDs,
and consumer acknowledgement only after the handler reports a committed effect.
RabbitMQ wiring and transactional inbox integration remain pending; no broker
client dependency is introduced until its version is centrally verified.
