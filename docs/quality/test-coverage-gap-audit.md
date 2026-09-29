# Repository-wide test coverage gap audit

**Task:** QA-10  
**Status:** In progress  
**Last audited:** 2026-09-29  
**Scope:** `app/`, `libs/`, `tests/`, `tools/bruno/`, contracts, and the
test/quality documentation.

## Purpose and completion rule

This document is the implementation-backed backlog for missing tests. It is
more demanding than a line-coverage report and more specific than the public
operation matrix. Every production branch must have a named assertion at the
lowest useful layer, and every externally observable behavior must also have
the strongest applicable transport or deployed test.

QA-10 is complete only when every row below has either:

1. a passing test linked to the exact production class/method and invariant;
2. a documented, reviewed reason that the branch is generated/structural and
   does not require behavior coverage; or
3. an environment-owned test result with its environment, threshold, artifact,
   and reviewer recorded.

Green compilation, a passing application-context test, a Bruno request, or a
JaCoCo percentage alone never closes a row. Mocked gateway/controller tests
prove mapping only; they do not prove deployed wiring, persistence, broker
delivery, identity-provider behavior, or cross-service side effects.

## Audit evidence and limits

The current source inventory contains 498 Kotlin production files and 122
Kotlin test files under applications and libraries. The generated local JaCoCo
reports currently report these line-coverage signals:

| Module | Missed lines | Covered lines | Signal |
| --- | ---: | ---: | --- |
| `app/accounts` | 235 | 1,153 | 83.1% |
| `app/bff` | 183 | 616 | 77.1% |
| `app/expense-core` | 239 | 1,999 | 89.3% |
| `app/notifications` | 115 | 481 | 80.7% |
| `libs/db` | 111 | 151 | 57.6% |
| `libs/errors` | 84 | 97 | 53.6% |
| `libs/ids` | 20 | 1 | 4.8% |
| `libs/observability` | 11 | 62 | 84.9% |
| `libs/security` | 68 | 84 | 55.3% |

These reports are discovery evidence and must be regenerated with the source
revision under review before closure. Kotlin compiler-generated accessors,
main functions, DTO constructors, and constants may explain some missed lines;
they still require an explicit classification rather than silent exclusion.

The operation matrix currently inventories 45 REST operations and 9 GraphQL
root operations. That proves inventory completeness, not complete validation,
authorization, failure, replay, concurrency, or side-effect coverage.

### Exhaustive current branch inventory

The regenerated JaCoCo XML contains **282 methods with at least one missed
branch**. This is the exhaustive discovery set for this revision; the summary
below prevents a high-level module percentage from hiding a small but important
method. Every method in this set must be assigned to a backlog row, tested, or
classified as generated/structural with reviewer approval.

| Module | Classes with missed lines | Classes with missed branches | Methods with missed branches |
| --- | ---: | ---: | ---: |
| `app/accounts` | 46 | 33 | 57 |
| `app/bff` | 28 | 24 | 49 |
| `app/expense-core` | 50 | 28 | 83 |
| `app/notifications` | 18 | 20 | 37 |
| `libs/db` | 12 | 14 | 27 |
| `libs/errors` | 5 | 4 | 10 |
| `libs/ids` | 3 | 0 | 0 |
| `libs/observability` | 2 | 3 | 10 |
| `libs/security` | 5 | 6 | 9 |
| **Total** | **169** | **132** | **282** |

The exact class, source file, method, source line, missed-branch count, and
covered-branch count are in the current files
`app/*/build/reports/jacoco/test/jacocoTestReport.xml` and
`libs/*/build/reports/jacoco/test/jacocoTestReport.xml`. Regenerate them with
`./gradlew.bat test jacocoTestReport --rerun-tasks --no-daemon`, then inspect
every `<class>/<method>/<counter type="BRANCH">` where `missed > 0`.
This is intentionally a fail-open discovery report: a missed branch is not
automatically a defect, but it is never silently treated as covered.

Methods named `<init>`, `equals`, `hashCode`, `toString`, Kotlin `$lambda$`,
and compiler-generated value/boxing methods require structural classification
only when the underlying production behavior is covered by an explicit test.
For example, testing a data class's equality behavior is valid; excluding all
`equals` methods merely because they are generated is not. Methods with domain,
transport, persistence, messaging, security, or configuration behavior must
receive a normal QA10 row even when JaCoCo reports partial coverage.

The 282-method inventory is a discovery baseline, not closure evidence. QA-10
cannot move to done until the inventory is rerun after each test increment and
the count is zero or every residual entry has a reviewed structural rationale.

## Required evidence ladder

Each production behavior is assigned the minimum evidence needed:

| ID | Layer | Required proof |
| --- | --- | --- |
| U | Unit/domain | Deterministic rule, boundary, exception, and invariant assertions without Spring or I/O |
| T | Controller/transport | HTTP/GraphQL status, schema/problem mapping, authentication, authorization, and request validation |
| P | Persistence integration | Real PostgreSQL/Flyway constraints, transaction boundaries, locking, versioning, and durable side effects |
| M | Messaging integration | Real RabbitMQ confirmation, ack/reject/requeue, deduplication, DLQ, retry, and outbox state transitions |
| E | Deployed E2E | Signed identity, real service boundaries, public response, durable state, and asynchronous effects |
| O | Operations/environment | Multi-replica, capacity, failover, restore, rotation, scan, alert, and rollback artifacts |

For a row marked `U+T+P+E`, all four layers are required. A unit test may be
listed as supporting evidence but cannot replace the higher layer.

## Missing or insufficient unit/transport/integration coverage

The following rows were identified by production-source inspection and missed
JaCoCo classes. The acceptance criteria are deliberately behavior-based so a
test cannot close a row by merely executing a line.

### Accounts and authentication

| ID | Production target | Missing/weak evidence | Required acceptance criteria |
| --- | --- | --- | --- |
| QA10-A01 | `AuthEmailOutboxPublisher.publishOne` | Local report has the publisher uncovered; no complete claim/serialize/publish lifecycle proof is visible in the focused test inventory. | `U+M`: empty claim returns `EMPTY`; valid event has the exact event type, schema version, recipient, encrypted credential, expiry, message ID, exchange, and routing key; successful publish acknowledges exactly once; AMQP and serialization failures reject with retry timing and bounded attempts; no plaintext credential is serialized or logged. |
| QA10-A02 | `OutboxAuthEmailSender.send` | Only partial line evidence; same-transaction append and envelope context need an explicit test. | `U+P`: recipient/template are used as AAD context; credential is encrypted before append; the raw credential never reaches the outbox; append failure leaves no successful result; returned status is `QUEUED` only after the append call. |
| QA10-A03 | `RedisRateLimitBucketStore.acquireAtomically` | Production Redis adapter is uncovered by local unit reports. | `P+E`: first request, window reset, cooldown denial, maximum denial, atomic concurrent callers, Redis nil result, and Redis exception are covered; keys are HMAC-derived/hex encoded, TTL is bounded, and store failure maps to fail-closed `429 RATE_LIMITED` with `Retry-After`. |
| QA10-A04 | `ExternalOidcTokenProvider.issueAccessToken`, `AuthSessionConfiguration` | Provider adapter is a throwing stub and must not be treated as completed OIDC integration. | `U+T+E`: either implement and exercise the real exchange/delegation contract, or prove the provider bean is absent/fails startup in every profile that cannot issue tokens; no runtime request may reach the current `UnsupportedOperationException`; production never silently falls back to an internal/test provider. |
| QA10-A05 | `ProductionSecurityConfig`, `OidcSubjectValidator`, JWT decoder wiring | Configuration classes have missed lines and current focused tests mostly validate policy helpers. | `T+E`: valid issuer/audience/algorithm/signature/subject succeeds; wrong issuer, audience, algorithm, signature, expiry, not-before, blank/oversized subject, missing JWKS, and unavailable issuer fail closed; all four deployed services use the intended decoder and no fallback decoder is active outside an explicitly test-only profile. |
| QA10-A06 | `ProfileController` profile/deletion/export routes | Several branches remain uncovered, especially nullable principals, object authorization, query context, and error mapping. | `T+P+E`: authenticated self-read/update succeeds; missing profile, blank/missing principal, foreign account, missing account, internal workload role, duplicate batch IDs, empty batch, over-limit batch, deletion request, export request, listing, and store failure each assert exact status/code and no unauthorized query or mutation side effect. |
| QA10-A07 | `TokenSessionService`, `LoginCredentialService`, cleanup and identity stores | Partial line coverage does not demonstrate replay, subject mismatch, deletion, expiry, or transaction behavior together. | `U+P+E`: one-time credential is single-use and expiry-bound; wrong recipient/subject, replay, session mismatch, revoked/deleted account, refresh rotation/reuse, logout, cleanup, and concurrent redemption produce exactly one durable outcome and the required redacted audit event. |

### BFF, GraphQL, and realtime

| ID | Production target | Missing/weak evidence | Required acceptance criteria |
| --- | --- | --- | --- |
| QA10-B01 | `AccountsGateway`, `ExpenseCoreGateway`, `RestGateway` | Missed lines remain in transport adapters; mocked mappings do not prove bearer/watermark/timeout behavior in a running BFF. | `U+T+E`: bearer token and request ID propagate, required watermark is forwarded and greatest valid downstream watermark is returned, 2-second timeout/connection/malformed JSON/non-2xx/empty body/partial result map to stable GraphQL extensions, and sensitive upstream details are redacted. |
| QA10-B02 | `GraphQlExceptionResolver`, `GraphQlLimitErrorInstrumentation`, scalar configuration | Existing HTTP transport tests do not cover every resolver/error and subscription limit branch. | `U+T+E`: every catalog error preserves code/source/component/operation/requestId/errorId; depth/complexity, per-subject query, mutation, and subscription caps reject deterministically; cancellation releases admission; scalar invalid/null/overflow values are rejected without resolver execution. |
| QA10-B03 | `LiveUpdateFanout`, `BffEventConsumer`, `RabbitBffEventListener` | Unit fanout evidence exists, but duplicate events, consumer ack behavior, revocation, reconnect replay, and broker failure need separate boundaries. | `U+M+E`: duplicate event IDs produce one invalidation, unrelated groups/subjects receive nothing, membership removal terminates active subscriptions, malformed/poison/transient messages are acked/rejected/requeued according to policy, and reconnect requires explicit cursor recovery with no duplicate financial event. |
| QA10-B04 | `BrowserOriginWebFilter`, `BrowserCsrfWebFilter`, cookie/session filters | In-process filter tests do not prove the complete browser-cookie mutation chain. | `T+E`: allowed origin plus matching CSRF succeeds; missing/mismatched token, disallowed origin, unsafe method, access-cookie mutation, bearer-only native client, preflight, and WebSocket upgrade follow the documented policy; no cookie/token is leaked in logs or responses. |

### Expense Core financial, scheduling, and sync logic

| ID | Production target | Missing/weak evidence | Required acceptance criteria |
| --- | --- | --- | --- |
| QA10-C01 | `JpaExpenseStore` command/query methods | Report shows missed lines in the largest financial adapter; successful lifecycle E2E does not cover every transaction rollback branch. | `U+P+E`: create/update/delete/idempotent replay/tampered replay, stale version, missing/archived group, unauthorized actor, invalid participant, audit row, sync revision, postings, balance, outbox, and rollback-after-each-write-failure are asserted; ledger remains zero-sum and no orphan records remain. |
| QA10-C02 | `ExpenseValidator`, `AllocationCalculator`, `FinancialArithmetic` | Missed branches include malformed amounts, overflow/rounding, and allocation boundaries. | `U`: property/table tests cover zero, negative, maximum, overflow, fractional/unknown currency, duplicate participants, missing payer, percentages not summing to 100, exact minor-unit remainder distribution, deterministic ordering, and zero-sum preservation with reproducible seeds. |
| QA10-C03 | `RecurringExpenseService`, `RecurringExpenseController`, worker/claim locking | Coverage exists for basic lifecycle but not all date, catch-up, lock, and generated-expense failure branches. | `U+P+E`: weekly/monthly/month-end clamp/timezone/end-date policy, pause/resume idempotency, missing/cross-group schedule, concurrent worker claims, bounded catch-up, deterministic occurrence IDs, duplicate prevention, failed occurrence rollback, and outbox/sync effects are asserted. |
| QA10-C04 | `JpaGroupStore`, invite/member lifecycle | Existing journey covers the happy path but not every replay/expiry/revocation and authorization transition. | `U+P+E`: invite expiry boundary, revoked/claimed/unknown token, simultaneous claim, placeholder binding, member removal, removed-member token/session, archived group, revision/audit/outbox effects, and non-member indistinguishable not-found behavior are asserted. |
| QA10-C05 | `JpaSettlementStore`, balances/suggestions/reconciliation | Settlement success and replay are covered, but independent corruption detection and rollback evidence are required. | `U+P+E`: valid settlement/reversal, same participant, negative/non-numeric/overflow amount, missing/cross-group settlement, duplicate idempotency key, concurrent reversal, balance recomputation from postings, intentional corruption detection, and zero-sum invariant are asserted. |
| QA10-C06 | `JpaSynchronizationStore`, sync controllers/cursors | Current E2E proves common cursor recovery, not all cursor ownership and transaction boundaries. | `U+P+E`: snapshot/change ordering, empty page, first/last page, limit bounds, malformed/expired/decreasing cursor, cross-group cursor, tombstone, membership loss, concurrent revision allocation, and no revision advance after rejected mutation are asserted. |

### Notifications and event delivery

| ID | Production target | Missing/weak evidence | Required acceptance criteria |
| --- | --- | --- | --- |
| QA10-D01 | `AuthEmailRabbitListener`, `AuthEmailDeliveryConsumer`, envelope parser | Parser/ack branches have partial coverage; listener behavior depends on RabbitMQ channel semantics. | `U+M+E`: valid encrypted envelope is consumed once; missing/wrong-type/malformed/expired payload is rejected without requeue; transient SMTP/decrypt failure requeues once then parks; duplicate event is acknowledged without a second email; no credential appears in logs or DLQ payloads. |
| QA10-D02 | `RabbitNotificationListener`, `TransactionalNotificationEventProcessor`, broker parser | Existing tests cover several outcomes but need explicit transaction/ack coupling and poison/retry matrix. | `U+M+E`: applied, duplicate, malformed, poison, transient, and permanent failures produce exact DB transaction and manual ack/reject/requeue outcome; inbox, processed-event, preference, and email side effects commit together or roll back together. |
| QA10-D03 | `SmtpMailSender`/mail adapter, `EmailDispatcher` | Adapter line coverage is low and no external Mailpit/SMTP failure matrix is documented. | `U+M+E`: valid message maps recipient/template/body correctly; invalid recipient and missing preference are suppressed; SMTP timeout/auth/rejection is classified as retryable/permanent; retry count, DLQ/parking, metrics, and redaction are asserted. |
| QA10-D04 | Notification inbox/preferences stores/controllers | Public smoke tests do not prove all persistence and failure branches. | `T+P+E`: subject isolation, default preferences, update versioning, invalid page/cursor bounds, unknown notification, duplicate mark-read, missing membership/context, reader fallback, and database failure map to exact public errors without changing another subject’s rows. |

### Shared libraries and cross-cutting infrastructure

| ID | Production target | Missing/weak evidence | Required acceptance criteria |
| --- | --- | --- | --- |
| QA10-E01 | `GlobalErrorHandler`, `RequestIdFilter`, `RequestIdContext` | `libs/errors` is 53.6% line-covered; the full exception/status/header/redaction matrix is not proven. | `U+T`: every catalog error, framework validation, malformed body, missing binding, type mismatch, media negotiation, optimistic conflict, unexpected exception, and rate-limit response asserts status, content type, stable code, source, request ID, bounded detail, and `Retry-After`; valid/invalid/oversized request IDs are propagated or replaced and MDC is cleared. |
| QA10-E02 | `DbAutoConfiguration`, `DbRoutingDataSource`, `DbOperationPolicy`, reader health/lag | `libs/db` is 57.6% line-covered and includes configuration/scheduler branches that unit tests cannot establish alone. | `U+P+E`: writer/reader route policy, command/strong/eventual query classification, fallback on lag/disconnect, recovery, causal watermark capture/validation, Flyway writer datasource, bounded pool acquisition, scheduler lifecycle, and no reader use for writes/locks/claims are asserted. |
| QA10-E03 | `OidcJwtDecoderFactory`, reactive decoder, headers | Security helper coverage is partial and does not prove servlet/reactive parity. | `U+T+E`: issuer, audience, algorithm, key source, temporal claims, invalid subject, key rotation overlap, missing metadata/JWKS, and security headers are identical across servlet and reactive services; failures never use a permissive decoder. |
| QA10-E04 | `ApiEndpoints`, event constants, IDs, error catalog | Low `libs/ids` JaCoCo signal is likely structural but unclassified. | `U`: contract/static tests enumerate every endpoint/event/error code exactly once, detect duplicate or drifted paths/routing keys, validate UUID/ID generation invariants, and document generated/accessor-only classes as excluded only with evidence. |
| QA10-E05 | `DbTelemetry`, observability adapters | Missed metric branches can hide unbounded labels or incorrect timing. | `U+T+O`: success/failure/slow query, pool acquisition, fallback, lock-wait, deadlock, and subscription metrics use bounded labels only; counters/timers increment exactly once and dashboards/alerts consume the same names. |

## Missing deployed E2E and environment tests

These cannot be closed by adding more in-process tests:

| ID | Missing E2E/operations proof | Acceptance criteria |
| --- | --- | --- |
| QA10-E2E01 | Complete per-operation authorization matrix | For every REST/GraphQL operation, signed owner/member/removed-member/non-member/workload personas prove allow/deny, object hiding, and no forbidden side effect; evidence names operation, token subject, status, error code, and durable state. |
| QA10-E2E02 | Auth email passwordless journey | Start login, consume the real outbox/broker path, receive Mailpit message, verify once-only code/link, expiry, replay denial, refresh/logout/revocation, and redacted logs using the provider actually enabled in the target profile. |
| QA10-E2E03 | Distributed rate-limit behavior | Run multiple service replicas against shared Redis; concurrent requests from the same and different normalized proxy addresses produce one atomic bucket, fail closed on Redis outage, recover after Redis restart, and retain bounded `Retry-After`. |
| QA10-E2E04 | WebSocket protocol completeness | Real `graphql-transport-ws` tests cover malformed RFC frames, malformed GraphQL payloads, duplicate subscription IDs, heartbeat timeout, sustained backpressure, membership revocation, reconnect/resubscribe cursor recovery, and no duplicate/lost invalidation. |
| QA10-E2E05 | Multi-replica and causal reads | Route requests across replicas, verify reader lag/fallback/recovery, writer watermark propagation, no stale strong reads, no sticky-session dependency, and event fanout consistency. |
| QA10-E2E06 | Capacity and failure domains | Approved production-like load runs record p50/p95/p99, error rate, pool saturation, queue depth, and reconciliation; thresholds are p50 <100ms, p95 <500ms, p99 <1s, error <0.1% for the documented mix. |
| QA10-E2E07 | Failover, restore, rotation, scanning, rollback, alerts | Staging/production-like artifacts prove database/broker failover, PITR restore, JWT/DB/broker secret rotation, image/dependency/secret scans, backward-compatible rollback, alert delivery, RPO <1 minute, RTO <5 minutes, and zero financial data loss. |

## Test implementation order

1. Close `QA10-E01`, `QA10-E02`, and `QA10-A04` first because every public
   assertion depends on correct error and authentication behavior.
2. Add the pure arithmetic, allocation, cursor, policy, and parser tests
   (`QA10-C02`, `QA10-C06`, `QA10-D01`, `QA10-E03`) before changing adapters.
3. Add PostgreSQL/RabbitMQ integration tests for transaction and message
   invariants (`QA10-A01`, `QA10-C01`, `QA10-C03`, `QA10-C05`, `QA10-D02`).
4. Extend operation-matrix evidence and live signed-persona E2E (`QA10-A06`,
   `QA10-B01`, `QA10-E2E01`–`QA10-E2E05`).
5. Execute environment-owned release gates only after the lower layers are
   green (`QA10-E2E06`–`QA10-E2E07`).

## Closure evidence required per increment

Each increment must record exact Gradle/Python/Compose commands, test counts,
artifact paths, source revision, and limitations in `docs/tasks/progress.md`.
The relevant operation-matrix rows and task detail must be updated in the same
increment. A failed or unavailable Docker/hosted run is recorded as unrun or
blocked; it is never converted into a mocked pass. JaCoCo must be regenerated
after tests are added, and every remaining missed class must be classified as
behavioral gap, infrastructure gap, or generated/structural code with a reason.
