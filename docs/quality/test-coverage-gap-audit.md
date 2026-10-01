# Repository-wide test coverage gap audit

**Task:** QA-10  
**Status:** In progress  
**Last audited:** 2026-10-01
**Scope:** `app/`, `libs/`, `tests/`, `tools/bruno/`, contracts, and the
test/quality documentation.

## Current baseline

As of 2026-10-01, the freshly regenerated JaCoCo XML baseline records **150
production methods with missed branches** containing **383 missed branches**,
**54 contract operations** (45 REST and 9 GraphQL), and **seven
environment-owned E2E/operations rows**. Operation source discovery
finds 41 operations without a literal E2E reference and 49 without a literal
Bruno reference. These numbers are backlog signals, not passing-test claims;
the hard branch gate remains red until the production reports are regenerated
against the current source and every record is covered or explicitly classified.
The current environment has Java 25 and the Gradle wrapper successfully ran
the repository-wide test and JaCoCo tasks. This baseline is local evidence;
hosted CI, deployed E2E, and environment-owned release gates remain separate
acceptance requirements.

The available Accounts report records 8 covered and 4 missed branches for
`DefaultRsaKeyProvider.loadOrGenerateKey`; this remains a provisional mapping
until the source revision is regenerated and the residual branches receive
coverage or an explicit structural classification.

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
| `app/accounts` | 74 | 1,314 | 94.7% |
| `app/bff` | 155 | 644 | 80.6% |
| `app/expense-core` | 169 | 2,070 | 92.5% |
| `app/notifications` | 37 | 559 | 93.8% |
| `libs/db` | 9 | 253 | 96.6% |
| `libs/errors` | 13 | 173 | 93.0% |
| `libs/ids` | 20 | 1 | 4.8% |
| `libs/observability` | 5 | 68 | 93.2% |
| `libs/security` | 36 | 116 | 76.3% |

These reports are discovery evidence and must be regenerated with the source
revision under review before closure. Kotlin compiler-generated accessors,
main functions, DTO constructors, and constants may explain some missed lines;
they still require an explicit classification rather than silent exclusion.

The operation matrix currently inventories 45 REST operations and 9 GraphQL
root operations. That proves inventory completeness, not complete validation,
authorization, failure, replay, concurrency, or side-effect coverage.

For repeatable operation-level discovery, run
`uv run --frozen --no-build python tools/coverage/report_operation_test_gaps.py --format markdown`.
The tool compares contract operation IDs and GraphQL root fields with literal
operation-name references in `tests/e2e/` and `tools/bruno/`. The current
inventory contains 54 operations; 41 have no E2E source signal and 49 have no
Bruno source signal. These are discovery signals only: a missing string can be
a naming mismatch, while a present string does not prove authorization,
negative behavior, persistence, messaging, replay, or deployed side effects.
References are matched as standalone identifiers rather than arbitrary
substrings, so `group` cannot be falsely credited by an unrelated `groups` or
`groupId` occurrence. The matcher intentionally favors a reviewable false
negative over a false positive that could hide a missing E2E journey.
Each signal must therefore be reconciled against the operation matrix and the
QA10-E2E acceptance rows before closure. Every record is assigned to
`QA10-E2E01`, whose acceptance criterion is the complete signed-persona
per-operation authorization and side-effect matrix.

### Current operations without a literal E2E source reference

The current scan identifies the following 41 operations for explicit E2E
implementation or source-reference reconciliation. “No literal reference” is
not proof that an operation is never exercised; it is a reproducible discovery
signal that must be resolved with an operation-specific test name, or with a
reviewed mapping when a shared journey intentionally covers it. Every listed
operation remains open under `QA10-E2E01` until the signed-persona acceptance
matrix records success, authorization denial, validation/failure behavior, and
the required durable or asynchronous side effect.

| Service | Missing E2E operation IDs |
| --- | --- |
| Accounts API | `getMe`, `getProfileById`, `getProfilesBatch`, `listExportRequests`, `logout`, `requestDeletion`, `requestExport`, `startLogin`, `updateMe`, `verifyLogin` |
| Expense Core API | `archiveGroup`, `claimInvite`, `createInvite`, `createPlaceholder`, `createRecurringSchedule`, `deleteExpense`, `exportExpenses`, `getBalances`, `getChanges`, `getGroup`, `getRecurringSchedule`, `getSettlementSuggestions`, `getSnapshot`, `listExpenses`, `listGroupMembers`, `listGroups`, `listRecurringSchedules`, `pauseRecurringSchedule`, `previewAllocation`, `recordSettlement`, `removeGroupMember`, `resumeRecurringSchedule`, `reverseSettlement`, `revokeInvite`, `searchExpenses`, `updateExpense`, `updateRecurringSchedule` |
| Notifications API | `getPreferences`, `listInbox`, `markAsRead`, `updatePreferences` |

### Operation-specific E2E acceptance matrix

The following matrix expands the generic dimensions into the side effects and
authorization invariants required for each missing-operation family. Every
operation ID in the preceding list belongs to exactly one row; the test name,
signed personas, and captured artifacts must be recorded against the individual
operation IDs, not only against the family.

| Operation family and operation IDs | Required E2E acceptance |
| --- | --- |
| Accounts profile: `getMe`, `getProfileById`, `getProfilesBatch`, `updateMe` | A signed subject reads/updates only its permitted profile; owner, non-owner, removed-member, missing-profile, duplicate-batch, empty-batch, malformed-ID, and over-limit cases return the contract error without cross-subject queries or mutations. Batch output is deduplicated and omits unknown IDs exactly as specified. |
| Accounts requests: `listExportRequests`, `requestDeletion`, `requestExport` | A signed subject creates and lists only its own durable request rows; repeated requests follow the documented idempotency/state transition, authorization failures create no row or outbox event, and export/deletion state is visible with the correct redaction and audit evidence. |
| Passwordless authentication: `startLogin`, `verifyLogin` | LINK and CODE journeys use a test mailbox/Mailpit artifact, issuance and delivery failures remain generic, rate limits return bounded `Retry-After`, credentials are single-use/expiry-bound, replay and wrong-subject redemption are indistinguishable, and successful verification creates exactly one stable identity/session. |
| Session ownership: `logout` | The signed subject can revoke only its own refresh-token family; blank, unknown, expired, mismatched, replayed, and deleted-account cases produce the documented no-op or unauthorized result, mutate no unrelated family, and leave an auditable redacted revocation event. |
| Expense groups/membership: `archiveGroup`, `claimInvite`, `createInvite`, `createPlaceholder`, `getGroup`, `listGroupMembers`, `listGroups`, `removeGroupMember`, `revokeInvite` | Owner/member/non-member/removed-member personas exercise lifecycle and object hiding; invite expiry, revocation, duplicate/concurrent claim, placeholder binding, archive restrictions, membership revision/audit, sync change, and notification/outbox side effects are asserted transactionally. |
| Expense financial/search: `deleteExpense`, `exportExpenses`, `getBalances`, `getSettlementSuggestions`, `listExpenses`, `previewAllocation`, `recordSettlement`, `reverseSettlement`, `searchExpenses`, `updateExpense` | Valid and rejected writes prove authorization, validation, stale-version/idempotency, zero-sum ledger/postings, balance and suggestion consistency, search cursor/limit/filter behavior, CSV formula safety, audit/sync/outbox effects, rollback, and no cross-group visibility. |
| Recurrence: `createRecurringSchedule`, `getRecurringSchedule`, `listRecurringSchedules`, `pauseRecurringSchedule`, `resumeRecurringSchedule`, `updateRecurringSchedule` | Schedule ownership, date/time-zone/month-end policy, missing/archived group, pause/resume/update idempotency, concurrent worker claim, bounded catch-up, deterministic occurrence identity, duplicate prevention, failed occurrence rollback, and notification/outbox/sync effects are captured. |
| Synchronization: `getChanges`, `getSnapshot` | Signed group members receive ordered revisions/tombstones and opaque cursors; empty/first/last/expired/malformed/decreasing/cross-group cursors, membership loss, and rejected-mutation revision behavior are asserted with no stale strong read. |
| Notifications: `getPreferences`, `listInbox`, `markAsRead`, `updatePreferences` | Preferences and inbox rows are isolated by subject, defaults/version conflicts are enforced, invalid page/cursor and duplicate mark-read behavior is stable, broker delivery/deduplication/retry reaches the durable inbox, and unauthorized requests create no mutation or notification side effect. |

The following operations already have a literal E2E source signal, but are
listed explicitly so source presence cannot be mistaken for complete
acceptance evidence:

| Operation | Required E2E acceptance |
| --- | --- |
| REST `refreshToken` | A signed refresh request rotates only the caller's valid session family; expired, revoked, replayed, mismatched, and deleted-account tokens are rejected or no-op exactly as contracted, with one durable family transition and no token leakage. |
| REST `createExpense` | Owner/member personas prove validation, idempotency, posting/revision/audit/outbox effects, rollback, zero-sum ledger state, and no cross-group mutation for rejected or replayed requests. |
| REST `createGroup` | The authenticated subject creates exactly one durable group with the documented owner membership, revision, audit, and event effects; duplicate, malformed, and unauthorized requests create no partial state. |
| REST `updateGroup` | Owner/member authorization, optimistic version behavior, archived-group policy, audit/revision/sync effects, and rejected-request immutability are asserted with signed personas. |
| GraphQL `createExpense` | The GraphQL mutation preserves REST financial invariants, maps catalog errors and request IDs, propagates authentication and causal context, and exposes no forbidden group or ledger data. |
| GraphQL `createGroup` | The mutation enforces signed-subject authorization, creates the documented durable owner state once, maps failures consistently, and emits the expected asynchronous event without duplication. |
| GraphQL `recordRepayment` | Valid, replayed, conflicting, unauthorized, and invalid repayment mutations assert exact GraphQL errors, posting/balance/revision effects, idempotency, and zero-sum preservation. |
| GraphQL `updateGroup` | Owner/member, removed-member, archived, stale-version, and malformed-input outcomes preserve the same authorization, mutation, audit, and error contracts as the REST path. |
| GraphQL query `group` | Signed owner/member/removed-member/non-member personas prove object hiding, field authorization, causal consistency, and no cross-group data exposure. |
| GraphQL query `groups` | Results are subject-scoped and stable under pagination, empty/invalid cursors, membership removal, and concurrent changes; hidden groups and sensitive fields never appear. |
| GraphQL query `me` | The token subject resolves only its own profile/session view; absent, invalid, revoked, and deleted identities return the documented redacted result without mutation. |
| GraphQL query `settlementSuggestions` | Suggestions respect group membership, ledger state, currency and pagination boundaries, expose no private data, and remain consistent after settlement/reversal. |
| GraphQL subscription `groupChanged` | A signed subscriber receives only permitted group changes, reconnects from an explicit cursor without duplicates or loss, and is terminated after membership revocation or invalid protocol state. |

The GraphQL roots currently have literal E2E references, but those references
still require the dimension checks above and in the operation matrix; source
presence is not acceptance evidence by itself. Regenerate this list with
`uv run --frozen --no-build python tools/coverage/report_operation_test_gaps.py --format markdown`.

For each operation, the E2E ledger must carry these dimensions separately:

| Dimension | Required evidence |
| --- | --- |
| Authentication | Valid signed persona reaches the intended boundary; missing, expired, wrong-issuer, and invalid-signature tokens are rejected without a side effect. |
| Authorization | Owner/member, removed member, non-member, and workload persona outcomes are recorded where the contract permits them; object hiding and exact error code are asserted. |
| Input and failure behavior | Required, blank, malformed, boundary, oversized, duplicate, stale-version, and unsupported values assert exact status/schema/problem code. |
| Durable state | PostgreSQL rows, revisions, audit records, idempotency state, and ledger invariants are checked after success and after rejection/rollback. |
| Asynchronous state | Outbox, broker acknowledgement/requeue/DLQ, inbox deduplication, Mailpit delivery, and eventual state are checked where applicable. |
| Replay and concurrency | Repeated and concurrent requests prove one durable outcome, no duplicate side effect, and the documented conflict/rate-limit behavior. |
| Isolation and redaction | Other subjects/groups cannot observe or mutate the result; credentials, tokens, and sensitive upstream details are absent from responses and logs. |

An operation with a source-reference signal is not closed until these dimensions
are evidenced. The 41 operations without a literal E2E reference are explicit
implementation/reconciliation work, while the remaining operations still need
the same dimension review rather than being inferred closed from a string match.

### Exhaustive current branch inventory

The regenerated JaCoCo XML contains **150 methods with at least one missed
branch**. This is the exhaustive discovery set for this revision; the summary
below prevents a high-level module percentage from hiding a small but important
method. Every method in this set must be assigned to a backlog row, tested, or
classified as generated/structural with reviewer approval.

| Module | Classes with missed lines | Classes with missed branches | Methods with missed branches |
| --- | ---: | ---: | ---: |
| `app/accounts` | 28 | 11 | 15 |
| `app/bff` | 24 | 18 | 39 |
| `app/expense-core` | 47 | 25 | 69 |
| `app/notifications` | 10 | 13 | 25 |
| `libs/db` | 7 | 2 | 2 |
| `libs/errors` | 1 | 1 | 1 |
| `libs/ids` | 3 | 0 | 0 |
| `libs/observability` | 2 | 0 | 0 |
| `libs/security` | 2 | 0 | 0 |
| **Total** | **124** | **70** | **150** |

The exact class, source file, method, source line, missed-branch count, and
covered-branch count are in the current files
`app/*/build/reports/jacoco/test/jacocoTestReport.xml` and
`libs/*/build/reports/jacoco/test/jacocoTestReport.xml`. Regenerate them with
`./gradlew.bat test jacocoTestReport --rerun-tasks --no-daemon`, then inspect
every `<class>/<method>/<counter type="BRANCH">` where `missed > 0`.
This is intentionally a fail-open discovery report: a missed branch is not
automatically a defect, but it is never silently treated as covered.

For a stable per-method inventory, run:

```text
uv run --frozen --no-build python tools/coverage/report_branch_gaps.py --format markdown
uv run --frozen --no-build python tools/coverage/report_branch_gaps.py --format json
# Closure gate: this must exit 0 only after every gap is closed or removed
# through an explicitly reviewed structural classification.
uv run --frozen --no-build python tools/coverage/report_branch_gaps.py --format json --fail-on-gaps
```

The JSON array is the machine-readable assignment set. Its record count must
equal the `Methods with missed branches` total above (**150**), and the sum of
its `missed_branches` fields must equal the current missed-branch total
(**383**). Each object carries the module, production class, source file,
method, source line, missed/covered branch counts, originating JaCoCo report,
provisional QA-10 row, and assignment basis. The record count and branch-count
sum are both regression-tested so a changed JaCoCo baseline cannot silently
replace one uncovered branch with another while appearing stable by method
count alone.
The tool emits no synthetic exclusions and returns all methods with `missed > 0`.
The provisional row is path-based accountability, not closure evidence;
reviewers must confirm the classification and then link each object to a
passing test or an explicitly reviewed generated/structural rationale.

### Per-record closure contract

The JSON inventory is the authoritative assignment set, but a QA row is not a
test record. For every inventory object, the closure ledger must record the
production module, class, source file, method, source line, missed and covered
branch counts, test file and test name, evidence layer, and the invariant or
failure outcome asserted. The linked test must exercise the behavior represented
by the missed branch; merely executing the method, increasing line coverage, or
asserting a generic HTTP status is insufficient. Persistence, messaging, and
deployed rows must additionally record durable state, broker acknowledgement or
retry state, token persona, and externally observable response that prove the
side effect.

If a regenerated report still contains a record, its method is not closed even
when a nearby test passes. A record may leave the open inventory only when the
next report shows no missed branches, or when the exact JaCoCo mapping is
reviewed and the ledger records why the remaining instrumentation is generated
or structurally unreachable, which source invariant makes it unreachable, and
the test that proves that invariant. Deleting code, weakening a guard, adding a
JaCoCo exclusion, or changing a contract does not satisfy this rule.

### Current per-record ledger status

The repository currently has the exact 150-record JSON discovery inventory and
row-level acceptance matrix, but it does **not** yet have closure evidence for
all 150 records. The A07 and E02 residual tables are the first exact method-level
ledger slices; the remaining records still require one of the following to be
recorded against the exact class/method/source line: a passing unit test, a
persistence/messaging integration test, a deployed E2E artifact, or a reviewed
structural rationale. This is an intentional open deliverable, not an implied
claim that the aggregate row counts close every branch.

Current provisional assignment workload (150 records):

| QA row | Branch-gap records | Primary missing evidence |
| --- | ---: | --- |
| QA10-A01 | 0 | Local publisher slice is complete; broker/deployed delivery remains required. |
| QA10-A02 | 0 | Auth-email sender, retry, parking, stale-event, and durable handoff branches are locally covered; real broker/deployed delivery remains required. |
| QA10-A03 | 1 | Shared Redis atomicity, outage, and public rate-limit behavior remain open; resolver and local policy boundaries are covered, with one structurally unreachable resolver fallback retained. |
| QA10-A04 | 0 | Explicit external identity-provider path; constructor and unsupported-delegation behavior are locally covered, deployed provider exchange remains required. |
| QA10-A05 | 1 | Local and non-local OIDC decoder selection, discovery, and algorithm wiring; the original decoder terminal branch is retained and needs explicit test evidence. |
| QA10-A06 | 1 | Profile controller and JPA persistence authorization boundary; restored private mapper requires classification or direct evidence. |
| QA10-A07 | 11 | Session, credential, identity, replay, and cleanup behavior. |
| QA10-A08 | 1 | Email canonicalization and malformed-input boundaries; restored explicit domain checks require direct boundary evidence. |
| QA10-B01 | 12 | BFF upstream transport and gateway failure behavior. |
| QA10-B02 | 15 | GraphQL resolver, error, scalar, and limit behavior. |
| QA10-B03 | 10 | Realtime fanout and broker consumer behavior. |
| QA10-B04 | 2 | Browser origin, CSRF, cookie, and session filters. |
| QA10-C01 | 40 | Expense persistence, transaction, ledger, and idempotency behavior. |
| QA10-C02 | 0 | Pure calculator/validator slice is branch-complete; property tests remain required. |
| QA10-C03 | 8 | Recurring schedules, claims, locking, and occurrence failures. |
| QA10-C04 | 9 | Group, invite, membership, expiry, and revocation behavior. |
| QA10-C05 | 8 | Settlement, balance, reconciliation, and rollback behavior. |
| QA10-C06 | 4 | Sync revisions, cursors, ordering, and membership boundaries. |
| QA10-D01 | 10 | Auth-email broker parsing, retry, deduplication, and delivery. |
| QA10-D02 | 3 | Notification event transaction and acknowledgement coupling. |
| QA10-D03 | 9 | SMTP/Mailpit delivery and retry classification. |
| QA10-D04 | 2 | Inbox/preferences persistence and subject isolation. |
| QA10-E01 | 1 | Error mapping, framework failures, headers, and correlation cleanup; restored status fallback requires explicit evidence. |
| QA10-E02 | 2 | Database routing, reader health, fallback, and operational lifecycle. |
| QA10-E03 | 0 | Servlet/reactive OIDC decoder construction and key-validation paths are locally covered; deployed issuer/provider behavior remains environment evidence. |
| QA10-E04 | 0 | IDs/constants have no current missed-branch methods; static contract checks remain required. |
| QA10-E05 | 0 | Bounded observability labels and metric behavior is branch-complete locally; dashboards/alerts and deployed cardinality remain operational evidence. |

This table is regenerated from the JSON assignment output; it is not a
coverage claim. A row closes only when its acceptance criteria and required
evidence layers pass, and the next regenerated inventory removes or classifies
its records. The `--fail-on-gaps` mode is the repository-level no-missed-branch
gate and must be part of the final QA-10 validation package.

### Reviewed structural branch candidates

The following residual JaCoCo branches have been reviewed against the current
source invariants. They remain present in the discovery inventory until a
regenerated report and reviewer sign-off records the classification; this table
does not delete code or create a coverage exclusion.

| Production target | Structural rationale | Required proof before classification |
| --- | --- | --- |
| `libs/errors/.../GlobalErrorHandler.kt:143`, `applicationException` | Every `ErrorCode.httpStatus` value is one of the valid statuses returned by `HttpStatus.resolve`; the `when` fallback is defensive against an enum value that cannot exist at runtime. Existing tests invoke every catalog code and assert its governed status. | Reconfirm the enum/status mapping from the current source and retain the exhaustive `ErrorCode` status test. |
| `app/accounts/.../FallbackJwtDecoder.kt:29`, `decode` | The decoder list is required non-empty. Each loop iteration either returns a `Jwt` or catches a `JwtException` and assigns `lastFailure`; after the loop, `lastFailure` is therefore non-null. Existing tests cover first success, later success, and final failure. | Retain the constructor invariant and three outcome tests; do not simplify the terminal guard. |
| `app/accounts/.../ClientAddressResolver.kt:89`, `normalizeToPartition` | `InetAddress.getByName` returns an `Inet4Address` or `Inet6Address` for the supported address families; the final `else` is defensive for a future JDK subtype. Existing tests cover IPv4, IPv6, malformed, and missing addresses. | Reconfirm the JDK address-family invariant and retain the family/malformed boundary tests. |
| `app/accounts/.../SessionPolicy.kt:82`, `isExpired` | `SessionExpiry` enforces `idleExpiresAt <= absoluteExpiresAt`. If the first short-circuit operand (`now + skew >= idle`) is false, the absolute boundary is necessarily later and the second operand is also false; if the absolute boundary is reached, idle expiry has already made the first operand true. Existing tests cover both observable expiry boundaries and skew. | Retain the `SessionExpiry` ordering invariant and the idle, absolute, and skew tests; confirm the JaCoCo residual is the unreachable short-circuit path after regeneration. |

The private `ProfileController.mapErrorCode` record is intentionally **not**
listed here: it is currently unreferenced rather than structurally unreachable.
It remains an open review item; deleting it or weakening the controller merely
to change JaCoCo would violate QA-10’s coverage-through-tests rule.

Methods named `<init>`, `equals`, `hashCode`, `toString`, Kotlin `$lambda$`,
and compiler-generated value/boxing methods require structural classification
only when the underlying production behavior is covered by an explicit test.
For example, testing a data class's equality behavior is valid; excluding all
`equals` methods merely because they are generated is not. Methods with domain,
transport, persistence, messaging, security, or configuration behavior must
receive a normal QA10 row even when JaCoCo reports partial coverage.

The 150-method inventory is a discovery baseline, not closure evidence. QA-10
cannot move to done until the inventory is rerun after each test increment and
the count is zero or every residual entry has a reviewed structural rationale.

### Closure increment: QA10-B02 bearer authorization boundary

`app/bff/src/test/kotlin/com/subhrodip/squarewise/bff/graphql/BearerAuthorizationTest.kt`
specifies the local contract for GraphQL bearer handling: the authorization
scheme is case-insensitive, surrounding whitespace is removed, missing or
malformed credentials are rejected, supported Spring Security principal forms
are extracted consistently, and blank or unknown subjects are not admitted.
The test must pass without forwarding a blank credential or treating an
unrecognized principal as authenticated. This closes only the pure helper
boundary; deployed GraphQL authentication, authorization, redaction, and
subscription-limit evidence remain required under QA10-B02 and QA10-E2E01.

`app/bff/src/test/kotlin/com/subhrodip/squarewise/bff/graphql/GraphQlLimitErrorInstrumentationTest.kt`
specifies that maximum query depth and complexity become `RATE_LIMITED` with a
retry hint, unrelated execution failures become `VALIDATION_FAILED`, and
already classified errors are preserved. This is instrumentation evidence;
the deployed depth/complexity admission thresholds and cancellation behavior
still require the GraphQL transport and E2E suites.

`GraphQlScalarConfigurationTest` also rejects unsupported literal types for
`MoneyMinor` and `DateTime`, in addition to malformed values. The scalar
acceptance remains a local coercion contract; schema-level request handling
must still be exercised through GraphQL transport tests.

### Closure increment: QA10-D01 broker-envelope validation boundary

`app/notifications/src/test/kotlin/com/subhrodip/squarewise/notifications/consumer/transport/BrokerEnvelopeParserTest.kt`
specifies the envelope contract for required UUID/string/integer/timestamp
metadata, positive schema and revision values, supported scalar payload
conversion, malformed JSON, and non-object payload rejection. It proves
invalid messages fail before notification delivery; RabbitMQ acknowledgement,
requeue, encryption, redaction, and Mailpit delivery remain separate
integration/E2E acceptance requirements.

`DeliveryPolicyTest` and `RetryPolicyTest` retain explicit local acceptance for
both enabled-channel combinations, duplicate suppression, success,
permanent-failure, bounded retry, and invalid attempt/configuration values.
These policy tests do not close the required durable inbox, broker, or SMTP
delivery evidence.

`EmailDispatcherTest` also specifies that unknown sender failures are permanent,
zero configured attempts still perform exactly one send, and recipient
whitespace is normalized before dispatch. SMTP timeout/authentication and
Mailpit retry evidence remain required at the integration/E2E layer.

`ExpenseSearchTest` now explicitly covers `decodeSearchCursor`: null cursors
remain absent, valid opaque cursors decode to the stored key, and malformed or
blank values return the catalogued validation error. Pagination ordering,
subject/group authorization, durable query behavior, and export transport
evidence remain separate acceptance requirements.

The same search unit suite now asserts all formula prefixes (`=`, `+`, `-`,
`@`) and CSV comma/quote/newline escaping. This protects local export
serialization against formula injection without claiming that authorized
deployed export behavior or durable query isolation is covered.

Search pagination now also rejects zero/over-limit pages and malformed
currency filters at the domain boundary; controller validation and authorized
PostgreSQL query behavior remain separate evidence layers.

### Closure increment: QA10-E01 error/correlation boundary

`libs/errors/src/test/kotlin/com/subhrodip/squarewise/errors/GlobalErrorHandlerTest.kt`
now checks all twelve catalog status mappings, problem metadata, the bounded
rate-limit retry header, and the bodyless 406 response. The new
`RequestIdContextAndFilterTest` checks valid propagation, invalid-ID replacement,
generated UUID response headers, cleanup after normal execution, and cleanup
after exceptions. The focused command passed 11 tests and the regenerated
module report now shows 99.4% line coverage and no missed branch methods in
the fresh report; deployed framework wiring and redaction branches remain
environment evidence requirements.

`AllocationCalculator` is the first branch-complete production slice after the
increment: its regenerated report has 0 missed lines, 0 missed branches, and 0
missed methods across `equal`, `exact`, `percentage`, `weightedShares`, and
`calculate`. At that first increment, the surrounding `ExpenseValidator` and
property-based financial arithmetic criteria remained open under QA10-C02.

The follow-up `ExpenseValidatorTest` increment now also reports 0 missed lines,
0 missed branches, and 0 missed methods for `parseAndValidateAmount`,
`validatePayers`, `mapDomainPayers`, and `mapDomainAllocations`. Financial
property tests outside these two pure components remain open.

The `AuthEmailOutboxPublisherTest` increment now covers the four local
publication outcomes: empty claim, successful JSON envelope publication and
acknowledgement, broker failure with bounded retry, and serialization failure
without a send. The focused publisher test passes, and the regenerated
Accounts report shows 0 missed lines, 0 missed branches, and 0 missed methods
for `AuthEmailOutboxPublisher.publishOne`. This is unit-level adapter evidence;
QA10-A01 remains open for real RabbitMQ confirmation/redelivery and the
deployed Notifications auth-email journey.

The `OutboxAuthEmailSenderTest` increment covers protection with the exact
recipient/template context and append failure propagation using the real
`AuthEmailOutboxService` against a repository double. The Spring
`AuthPersistenceTest` now also invokes the production sender and verifies a
real transactional outbox row contains only a decryptable protected envelope,
the expected template/expiry, and `PENDING` status. The target sender reports
0 missed lines and methods. QA10-A02 still requires a failure-in-transaction
rollback assertion if the broader transaction boundary changes; the current
integration test proves persistence and cryptographic handoff, not broker
delivery.

The `RedisRateLimitBucketStoreTest` increment covers the adapter's atomic
script invocation, hex-key handoff, epoch arguments, bounded TTL, allow result,
null-result fail-closed path, and Redis-exception wrapping. The regenerated
Accounts report shows 0 missed lines, branches, and methods for
`acquireAtomically`. This remains mocked-adapter evidence; QA10-A03 still
requires a live shared-Redis concurrency/window-reset/outage test and public
multi-replica 429 evidence.

The `DbOperationPolicyTest` increment now executes under the Gradle wrapper and
covers valid writer/reader routes, operation-name grammar rejection, reader
eligibility for non-query kinds, and the strong-consistency/reader conflict.
The remaining constructor record is retained for structural review; it is not
closed by deleting or simplifying the policy invariants.

### Current QA10-E02 residual acceptance targets

The current regenerated inventory contains two E02 records. They remain
explicitly open until the following evidence is attached:

| Production target | Current evidence | Required closure evidence |
| --- | --- | --- |
| `DbReaderHealth.state` | Open-circuit before-expiry, exact-deadline, and after-expiry behavior are now directly asserted; residual JaCoCo branches require source/bytecode classification. | Preserve all three timing boundaries and classify only compiler/nullability-generated paths after reviewing the report mapping, or add a behavior test if a reachable state is identified. |
| `DbOperationPolicy::<init>` | All policy invariants and valid writer/reader routes are asserted in `DbOperationPolicyTest`. | Review the constructor branch mapping; retain the invariant tests and classify only generated short-circuit/data-class instrumentation, never remove a policy guard to change the count. |
| `DbContextHolder.withContext` | Nested restoration, exception restoration, request-watermark inheritance, and explicit-watermark precedence are asserted. | Review the remaining JaCoCo branch against the nullable ThreadLocal/causal-context paths; add only a reachable restoration/inheritance case, otherwise record a structural rationale with the exact source branch. |

### Current QA10-A07 method-level residual ledger

The following records are the exact A07 assignment from the current JaCoCo
inventory. Counts are discovery values, not closure claims. Each row must be
rechecked after the corresponding tests run; a test specification does not
remove a record until a regenerated report does so.

| Production target and source line | Method | Missed branches | Required evidence or classification |
| --- | --- | ---: | --- |
| `OneTimeCredentialIssuer.kt:49` | `IssuedCredential.equals` | 7 | `OneTimeCredentialIssuerTest` now tests byte-array equality plus every differing field, null, and unrelated-type cases; retain the method and classify only any remaining compiler instrumentation after the regenerated mapping. |
| `AesGcmCredentialEnvelopeProtector.kt:38` | `reveal` | 1 | Existing `CredentialEnvelopeProtectorTest` covers round trip, context mismatch, tampering, malformed Base64, truncation, and unsupported version; map the remaining defensive `GeneralSecurityException` catch before classifying it as structural. |
| `AccountIdentityStore.kt:70` | default `updateEmail` | 2 | `AccountIdentityStoreTest` now verifies the existing-identity return contract and the exact missing-account exception; retain the default implementation. |
| `JpaAccountIdentityStore.kt:33` | `findByAccountId` | 7 | Test durable identity, profile fallback, missing profile, and deletion flag mapping against PostgreSQL. |
| `JpaAccountIdentityStore.kt:54` | `findByIssuerAndSubject` | 3 | Test matching and unknown composite identity, including nullable email mapping. |
| `JpaAccountIdentityStore.kt:74` | `findByEmail` | 2 | Test case-insensitive match, unknown email, and duplicate/active identity selection contract. |
| `JpaAccountIdentityStore.kt:104` | `enrollIdentity` | 3 | Test first enrollment and re-enrollment uniqueness/contact/verification persistence. |
| `JpaAccountIdentityStore.kt:145` | `updateEmail` | 2 | Test existing-account update and missing-account failure with durable email replacement. |
| `DefaultRsaKeyProvider.kt:62` | `rotateKey` | 1 | Existing `DefaultRsaKeyProviderTest` covers valid replacement, overlap retention, public-only rejection, blank/missing `kid`, and successive rotations; review the remaining JaCoCo mapping before classifying it as structural. |
| `DefaultRsaKeyProvider.kt:73` | `loadOrGenerateKey` | 4 | `DefaultRsaKeyProviderTest` now covers complete PEM loading, private-only/public-only fallback generation, and malformed complete PEM rejection; regenerated JaCoCo must confirm the remaining mapping. |
| `LoginStartService.kt:40` | `start` | 2 | Execute LINK/CODE mapping, issuance failure, ordinary denial, store outage, and generic response behavior. |
| `LoginVerificationService.kt:60` | `verify` | 2 | Execute invalid/replayed credential and existing-identity reuse versus new enrollment with durable session state. |
| `AsymmetricJwtTokenProvider.kt:28` | constructor | 4 | Execute blank issuer and audience guards; retain valid configuration issuance test. |
| `SessionExpiry.kt:14` | constructor | 2 | `SessionExpiryTest` now directly verifies equal boundaries and rejects an idle boundary later than the absolute boundary; retain the invariant and classify only compiler-generated record instrumentation after report mapping. |
| `SessionPolicy.kt:82` | `isExpired` | 1 | Existing `SessionPolicyTest` covers exact idle/absolute expiry and clock-skew boundaries; classify only the remaining short-circuit path if the regenerated mapping proves it unreachable under the constructor invariant. |
| `TokenSessionService.kt:116` | `rotateSession` | 4 | `TokenSessionServiceRaceTest` now specifies `rotateIfActive == 0` fail-closed family revocation with no token mint; integration/E2E evidence must still exercise a real concurrent race, plus missing account/identity, subject mismatch, deletion, and expiry. |
| `TokenSessionService.kt:209` | `revokeSessionByRefreshToken` | 1 | `TokenSessionServiceTest` now also covers a legacy session with no account ID: authenticated-subject revocation returns without family mutation; retain blank, unknown, missing-identity, mismatch, and matching-subject outcomes. |
| `JpaDeletionRequestStore.kt:46` | `request` | 2 | `JpaDeletionRequestStoreTest` now verifies first-request persistence when profile lookup returns no account, including profile marking and no session-revocation interaction; retain integration coverage for idempotency, profile preservation, and account-backed session revocation. |
| `JpaDeletionRequestStore.kt:87` | `cancel` | 1 | Test missing record returns null and existing record persists `CANCELLED`. |
| `JpaDeletionRequestStore.kt:101` | `complete` | 1 | Test missing record returns null and existing record persists `COMPLETED`. |

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

## Concrete test destinations

Each backlog row has an owned test destination. Existing files are extended
only when their responsibility matches; otherwise the named file is the
planned new test. This prevents a broad suite from absorbing an unrelated gap.

| Row | Unit/transport destination | Integration/E2E destination |
| --- | --- | --- |
| QA10-A01 | `app/accounts/src/test/kotlin/com/subhrodip/squarewise/accounts/auth/delivery/service/AuthEmailOutboxPublisherTest.kt` | Extend `app/notifications/src/test/kotlin/com/subhrodip/squarewise/notifications/email/AuthEmailRabbitListenerTest.kt`; add `tests/e2e/test_auth_email_delivery.py`. |
| QA10-A02 | `app/accounts/src/test/kotlin/com/subhrodip/squarewise/accounts/auth/delivery/service/OutboxAuthEmailSenderTest.kt` | `app/accounts/src/test/kotlin/com/subhrodip/squarewise/accounts/auth/AuthPersistenceTest.kt`; add caller-transaction rollback coverage. |
| QA10-A03 | `app/accounts/src/test/kotlin/com/subhrodip/squarewise/accounts/auth/abuse/RedisRateLimitBucketStoreTest.kt` | Add `tests/e2e/test_distributed_rate_limit.py` against shared Redis and replicas. |
| QA10-A04 | Add `app/accounts/src/test/kotlin/com/subhrodip/squarewise/accounts/auth/provider/ExternalOidcTokenProviderTest.kt` | Extend `app/accounts/src/test/kotlin/com/subhrodip/squarewise/accounts/auth/login/DeployedPasswordlessTokenIntegrationTest.kt` with explicitly enabled-provider startup/issuance. |
| QA10-A05 | `app/accounts/src/test/kotlin/com/subhrodip/squarewise/accounts/security/OidcSubjectValidatorTest.kt`; add deployed security configuration tests | `tests/e2e/test_oidc_negative.py` and signed multi-service decoder probes. |
| QA10-A06 | `app/accounts/src/test/kotlin/com/subhrodip/squarewise/accounts/profile/ProfileControllerTest.kt` | Add signed-persona profile authorization cases to `tests/e2e/test_rest_edge_cases.py`. |
| QA10-A07 | `app/accounts/src/test/kotlin/com/subhrodip/squarewise/accounts/auth/session/TokenSessionServiceTest.kt` and `app/accounts/src/test/kotlin/com/subhrodip/squarewise/accounts/auth/login/LoginVerificationServiceTest.kt` | Extend passwordless journey in `tests/e2e/test_product_journey.py`; assert durable replay/revocation state. |
| QA10-A08 | `app/accounts/src/test/kotlin/com/subhrodip/squarewise/accounts/auth/identity/EmailAddressTest.kt` | Not a deployed boundary; retain deterministic unit/property coverage. |
| QA10-B01 | `app/bff/src/test/kotlin/com/subhrodip/squarewise/bff/RestGatewayTest.kt`, `app/bff/src/test/kotlin/com/subhrodip/squarewise/bff/BffFanoutTest.kt`, and `app/bff/src/test/kotlin/com/subhrodip/squarewise/bff/transport/BffGatewayFiltersTest.kt` | Add upstream timeout/malformed-response cases to `tests/e2e/test_rest_edge_cases.py`. |
| QA10-B02 | `app/bff/src/test/kotlin/com/subhrodip/squarewise/bff/config/BearerTokenContextWebFilterTest.kt`, `app/bff/src/test/kotlin/com/subhrodip/squarewise/bff/graphql/BearerAuthorizationTest.kt`, `app/bff/src/test/kotlin/com/subhrodip/squarewise/bff/graphql/GraphQlLimitErrorInstrumentationTest.kt`, `app/bff/src/test/kotlin/com/subhrodip/squarewise/bff/graphql/GraphqlHttpTransportTest.kt`, and `app/bff/src/test/kotlin/com/subhrodip/squarewise/bff/graphql/GraphQlScalarConfigurationTest.kt` | `BearerTokenContextWebFilterTest` now proves case-insensitive trimmed bearer capture, valid watermark capture, malformed-header/watermark omission, and exchange retention. Extend GraphQL HTTP and WebSocket suites with every limit/error dimension; prove bearer extraction, subject normalization, and public limit-error mapping at the unit boundary. |
| QA10-B03 | `app/bff/src/test/kotlin/com/subhrodip/squarewise/bff/LiveUpdateFanoutTest.kt`, `app/bff/src/test/kotlin/com/subhrodip/squarewise/bff/messaging/BffEventConsumerTest.kt`, and `app/bff/src/test/kotlin/com/subhrodip/squarewise/bff/messaging/RabbitBffEventListenerTest.kt` | Extend `tests/e2e/test_concurrency_subscriptions.py` and add reconnect/replay cases. |
| QA10-B04 | `app/bff/src/test/kotlin/com/subhrodip/squarewise/bff/config/BrowserCsrfWebFilterTest.kt`, `app/bff/src/test/kotlin/com/subhrodip/squarewise/bff/config/BrowserOriginPolicyTest.kt`, and cookie filter tests | Add browser-cookie mutation and WebSocket upgrade cases to the public E2E harness. |
| QA10-C01 | `app/expense-core/src/test/kotlin/com/subhrodip/squarewise/expensecore/expenses/JpaExpenseStoreTest.kt`, `JpaExpenseStoreTransactionRollbackTest`, and controller tests | `JpaExpenseStoreTest` now covers actor-scoped active membership identifiers and duplicate/inactive participant rejection before mutation; `JpaExpenseStoreTransactionRollbackTest` injects an outbox-port failure and asserts expense, idempotency, postings, sync, and group revision rollback. Extend `tests/e2e/test_product_journey.py` with deployed write-failure rollback and ledger reconciliation. |
| QA10-C02 | `app/expense-core/src/test/kotlin/com/subhrodip/squarewise/expensecore/expenses/AllocationCalculatorTest.kt` and `app/expense-core/src/test/kotlin/com/subhrodip/squarewise/expensecore/expenses/ExpenseValidatorTest.kt` | Add deterministic property/table tests in the same package. |
| QA10-C03 | `app/expense-core/src/test/kotlin/com/subhrodip/squarewise/expensecore/recurring/RecurringExpenseServiceTest.kt`, `app/expense-core/src/test/kotlin/com/subhrodip/squarewise/expensecore/recurring/RecurringExpenseWorkerTest.kt`, and `app/expense-core/src/test/kotlin/com/subhrodip/squarewise/expensecore/recurring/RecurringExpenseControllerTest.kt` | Add multi-worker and bounded catch-up cases to the Docker E2E suite. |
| QA10-C04 | `app/expense-core/src/test/kotlin/com/subhrodip/squarewise/expensecore/groups/JpaGroupStoreTest.kt` and `app/expense-core/src/test/kotlin/com/subhrodip/squarewise/expensecore/groups/GroupControllerTest.kt`; the JPA suite now covers archived member operations, invalid placeholder/invite tokens, and unknown membership removal without additional effects. | Extend signed-persona lifecycle coverage in `tests/e2e/test_product_journey.py`. |
| QA10-C05 | `app/expense-core/src/test/kotlin/com/subhrodip/squarewise/expensecore/settlements/JpaSettlementStoreTest.kt`, `app/expense-core/src/test/kotlin/com/subhrodip/squarewise/expensecore/settlements/SettlementServiceTest.kt`, and `app/expense-core/src/test/kotlin/com/subhrodip/squarewise/expensecore/settlements/PostgresSettlementReconciliationTest.kt` | Existing JPA and opt-in PostgreSQL tests cover settlement/reversal postings, replay, cardinality, directional signs, and zero-sum totals. Extend financial lifecycle E2E and reconciliation operations with intentional-corruption detection and rollback/failure evidence. |
| QA10-C06 | `app/expense-core/src/test/kotlin/com/subhrodip/squarewise/expensecore/sync/JpaSynchronizationStoreTest.kt`, `SynchronizationTest.kt`, and `SyncControllerTest.kt`; the suites now cover blank identifiers, invalid limits, malformed cursor fields, and blank authenticated subjects. | Extend `tests/e2e/test_offline_resilience.py` and causal cursor probes. |
| QA10-D01 | `app/notifications/src/test/kotlin/com/subhrodip/squarewise/notifications/consumer/transport/BrokerEnvelopeParserTest.kt`, `AuthEmailRabbitListenerTest.kt`, `AuthEmailDeliveryConsumerTest.kt`, and envelope tests | Add broker/Mailpit auth-email delivery to `tests/e2e/test_auth_email_delivery.py`; consumer tests must prove template mapping, expiry/type rejection before reveal, context-bound decryption, plaintext non-dispatch on reveal failure, and listener ack/requeue behavior. |
| QA10-D02 | `app/notifications/src/test/kotlin/com/subhrodip/squarewise/notifications/consumer/RabbitNotificationListenerTest.kt`, `BrokerEnvelopeConversionTest.kt`, `NotificationEventConsumerTest.kt`, `NotificationEventConsumerUnitTest.kt`, and `RedisDeliveryRateLimiterTest.kt` | The conversion test now covers malformed notification-ID fallback, recipient/description mapping, and bounded fields; add real RabbitMQ ack/retry/DLQ and shared-Redis TTL/concurrency cases to the chaos E2E suite. |
| QA10-D03 | `app/notifications/src/test/kotlin/com/subhrodip/squarewise/notifications/email/EmailDispatcherTest.kt`, `app/notifications/src/test/kotlin/com/subhrodip/squarewise/notifications/email/smtp/SmtpJavaMailSenderTest.kt`, and `app/notifications/src/test/kotlin/com/subhrodip/squarewise/notifications/email/smtp/SimpleMailMessageTest.kt` | Add Mailpit failure/retry assertions to deployed notification E2E. |
| QA10-D04 | `app/notifications/src/test/kotlin/com/subhrodip/squarewise/notifications/inbox/InboxControllerTest.kt`, `JpaNotificationInboxStoreTest.kt`, and `app/notifications/src/test/kotlin/com/subhrodip/squarewise/notifications/preferences/JpaPreferenceStoreTest.kt` | Extend signed subject-isolation cases in REST-edge E2E. |
| QA10-E01 | `libs/errors/src/test/kotlin/com/subhrodip/squarewise/errors/GlobalErrorHandlerTest.kt` and `libs/errors/src/test/kotlin/com/subhrodip/squarewise/errors/request/RequestIdContextAndFilterTest.kt` | Assert public error envelopes and correlation behavior in REST/GraphQL E2E; the available JaCoCo baseline is not a fresh current-source report. |
| QA10-E02 | `libs/db/src/test/kotlin/com/subhrodip/squarewise/db/config/DbAutoConfigurationTest.kt`, `libs/db/src/test/kotlin/com/subhrodip/squarewise/db/policy/DbOperationPolicyTest.kt`, and health/routing tests | `DbAutoConfigurationTest` now covers normal reader-pool construction and `DbReaderHealthTest` covers the exact circuit deadline; `DbRouteGuardTest` asserts exception restoration. Extend `tests/e2e/test_causal_watermark.py` and replica failure/recovery suites. |
| QA10-E03 | `libs/security/src/test/kotlin/com/subhrodip/squarewise/security/OidcJwtDecoderFactoryTest.kt` and policy tests | Extend signed invalid-token and key-rotation E2E across all services. |
| QA10-E04 | `libs/ids/src/test/kotlin/com/subhrodip/squarewise/ids/UuidGeneratorTest.kt`; add static contract assertions | Contract validator and public-surface validation remain the integration destination. |
| QA10-E05 | `libs/observability/src/test/kotlin/com/subhrodip/squarewise/observability/db/DbTelemetryTest.kt` and observability tests | Add bounded-label, alert, and capacity evidence under QA10-E2E06/E2E07. |

### Deployed E2E and environment destinations

| Row | Existing suite to extend | Dedicated destination or artifact still required |
| --- | --- | --- |
| QA10-E2E01 | `tests/e2e/test_product_journey.py`, `test_rest_edge_cases.py`, and `test_oidc_negative.py` | Add `tests/e2e/test_operation_authorization_matrix.py` covering every REST/GraphQL operation and signed persona. |
| QA10-E2E02 | `tests/e2e/test_product_journey.py`, Mailpit/Compose harness | Add `tests/e2e/test_auth_email_delivery.py` for real outbox, broker, Mailpit, replay, expiry, refresh, logout, and redaction. |
| QA10-E2E03 | `tests/e2e/test_rest_edge_cases.py` and Compose Redis topology | Add `tests/e2e/test_distributed_rate_limit.py` for concurrent replicas, normalized proxy identity, outage, recovery, and bounded `Retry-After`. |
| QA10-E2E04 | `tests/e2e/test_concurrency_subscriptions.py` and `test_oidc_websocket_negative.py` | Add protocol cases for duplicate IDs, malformed payloads, heartbeat timeout, sustained backpressure, reconnect cursor recovery, and membership revocation. |
| QA10-E2E05 | `tests/e2e/test_causal_watermark.py`, replica smoke scripts, and `test_concurrency_subscriptions.py` | Preserve artifacts for routing, lag/fallback/recovery, watermark monotonicity, no stale strong reads, and fanout consistency. |
| QA10-E2E06 | `tests/load/k6/`, `tests/performance/capacity-smoke.sh`, and documented production-validation procedures | Produce approved p50/p95/p99, error-rate, pool, queue, and reconciliation artifacts for the declared workload and thresholds. |
| QA10-E2E07 | `tests/e2e/test_chaos_recovery.py`, `tests/performance/recovery-drill.sh`, and release-gate tooling | Produce reviewed failover, PITR restore, rotation, scanning, rollback, alert, RPO/RTO, and zero-loss artifacts in a production-like environment. |

## Missing or insufficient unit/transport/integration coverage

The following rows were identified by production-source inspection and missed
JaCoCo classes. The acceptance criteria are deliberately behavior-based so a
test cannot close a row by merely executing a line.

### Accounts and authentication

| ID | Production target | Missing/weak evidence | Required acceptance criteria |
| --- | --- | --- | --- |
| QA10-A01 | `AuthEmailOutboxPublisher.publishOne` | Unit coverage now exercises the complete local claim/serialize/send state machine; real RabbitMQ confirmation, redelivery, and deployed auth-email delivery remain open. | `U+M`: empty claim returns `EMPTY`; valid event has the exact event type, schema version, recipient, encrypted credential, expiry, message ID, exchange, and routing key; successful publish acknowledges exactly once; AMQP and serialization failures reject with retry timing and bounded attempts; no plaintext credential is serialized or logged. The unit increment passes all four cases and reports 0 missed lines, branches, and methods for `publishOne`; a real-broker/deployed test must still prove confirmation, retry/redelivery, and downstream delivery. |
| QA10-A02 | `OutboxAuthEmailSender.send` | Unit and Spring persistence coverage now prove context-bound encryption, protected-only durable storage, `PENDING` state, expiry/template mapping, and append failure propagation. A broader caller transaction rollback test and real broker path remain open. | `U+P`: recipient/template are used as AAD context; credential is encrypted before append; the raw credential never reaches the outbox; append failure leaves no successful result; returned status is `QUEUED` only after the append call. The current increment passes the unit and Spring persistence cases; add a caller transaction rollback test if sender invocation is part of a larger credential transaction. |
| QA10-A03 | `RedisRateLimitBucketStore.acquireAtomically` | Unit adapter coverage now proves script arguments, key encoding, TTL, null result, and exception wrapping; live Redis atomicity and public distributed behavior remain open. | `P+E`: first request, window reset, cooldown denial, maximum denial, atomic concurrent callers, Redis nil result, and Redis exception are covered; keys are HMAC-derived/hex encoded, TTL is bounded, and store failure maps to fail-closed `429 RATE_LIMITED` with `Retry-After`. The unit increment passes 3 adapter cases and reports 0 missed lines, branches, and methods; live shared-Redis and multi-replica tests must still prove window/concurrency/outage behavior. |
| QA10-A04 | `ExternalOidcTokenProvider.issueAccessToken`, `AuthSessionConfiguration` | The default deployed path now selects `AsymmetricJwtTokenProvider` and `DeployedPasswordlessTokenIntegrationTest` proves RS256 issuance/validation, but explicitly enabling `squarewise.security.oidc.external-provider.enabled=true` still selects the throwing external adapter. The optional external-provider path is therefore not a completed OIDC exchange. | `U+T+E`: either implement and exercise the real exchange/delegation contract, or reject/disable the external-provider property at startup until it is implemented; no runtime request may reach the current `UnsupportedOperationException`; production never silently falls back to an internal/test provider. Acceptance must include default and explicitly enabled-provider profiles, bean selection, startup behavior, token issuance, and deployed validation. |
| QA10-A05 | `ProductionSecurityConfig`, `OidcSubjectValidator`, JWT decoder wiring | Configuration classes have missed lines and current focused tests mostly validate policy helpers. | `T+E`: valid issuer/audience/algorithm/signature/subject succeeds; wrong issuer, audience, algorithm, signature, expiry, not-before, blank/oversized subject, missing JWKS, and unavailable issuer fail closed; all four deployed services use the intended decoder and no fallback decoder is active outside an explicitly test-only profile. |
| QA10-A06 | `ProfileController` profile/deletion/export routes and `JpaDeletionRequestStore` | Several branches remain uncovered, especially nullable principals, object authorization, query context, error mapping, and missing deletion-record lifecycle outcomes. `JpaRequestStoresTest` now specifies null results for missing deletion records and exact invalid-subject rejection; `JpaDeletionRequestStoreTest` covers a valid request with no resolvable profile account and proves session revocation is skipped while the request is persisted. | `T+P+E`: authenticated self-read/update succeeds; missing profile, blank/missing principal, foreign account, missing account, internal workload role, duplicate batch IDs, empty batch, over-limit batch, deletion request, export request, listing, missing deletion record, invalid subject, and store failure each assert exact status/code and no unauthorized query or mutation side effect. |
| QA10-A07 | `AsymmetricJwtTokenProvider`, `LoginStartService`, `LoginVerificationService`, `TokenSessionService`, `LoginCredentialService`, cleanup and identity stores | Partial line coverage does not demonstrate blank issuer/audience guards, existing-identity reuse, generic rate-limit failure, credential-delivery failure, replay, subject mismatch, deletion, expiry, or transaction behavior together. `AsymmetricJwtTokenProviderTest` now specifies both constructor guards; `LoginStartServiceTest` specifies both delivery templates, generic issuance failure, rate-limit denial, and rate-limit-store outage; `LoginVerificationServiceTest` specifies reuse of an enrolled identity without provisioning a second account. Focused tests still require execution after Gradle bootstrap is repaired. | `U+P+E`: token configuration rejects blank issuer/audience; an enrolled identity reuses its stable account and subject; one-time credential is single-use and expiry-bound; login admission and delivery failures remain generic and use stable `ERR_11`; wrong recipient/subject, replay, session mismatch, revoked/deleted account, refresh rotation/reuse, logout, cleanup, and concurrent redemption produce exactly one durable outcome and the required redacted audit event. The Accounts persistence suite now also proves expired-session family revocation, deletion-request denial with family revocation, missing-identity fail-closed rejection without an unintended family mutation, matching-subject logout revocation, and blank/unknown/missing-identity logout no-ops; deployed replay/concurrency evidence remains open. |
| QA10-A08 | `EmailAddress.parse` | Existing tests cover canonicalization and common malformed input; boundary tests now add exact maximum local/complete lengths and invalid IDN label separators. JaCoCo still reports 11 missed branches until a fresh report is generated. | `U`: NFC/root-locale canonicalization and IDN conversion are deterministic; empty/overlong local parts, total length, control/whitespace characters, malformed separators, empty or repeated domain labels, invalid IDN input, and labels over 63 characters all reject with `IllegalArgumentException`; accepted output contains exactly one canonical separator and no raw credential material. |

### BFF, GraphQL, and realtime

| ID | Production target | Missing/weak evidence | Required acceptance criteria |
| --- | --- | --- | --- |
| QA10-B01 | `AccountsGateway`, `ExpenseCoreGateway`, `RestGateway`, `BffGatewayFilters` | `AccountsGatewayTest` now exercises optional bearer omission/presence, profile decoding, and upstream status redaction; `BffFanoutTest` covers the main Expense Core HTTP-double paths; `BffGatewayFiltersTest` covers bearer/watermark forwarding, greatest-valid watermark retention, blank values, malformed downstream watermark, and missing exchange context. Running-BFF timeout/connection and complete operation failure evidence remain open. | `U+T+E`: bearer token and request ID propagate, required watermark is forwarded and greatest valid downstream watermark is returned, no-context requests remain credential-free, 2-second timeout/connection/malformed JSON/non-2xx/empty body/partial result map to stable GraphQL extensions, and sensitive upstream details are redacted. |
| QA10-B02 | `BearerTokenContextWebFilter`, `BearerAuthorization`, `GraphQlExceptionResolver`, `GraphQlLimitErrorInstrumentation`, scalar configuration | `BearerTokenContextWebFilterTest` now covers bearer/watermark context capture and malformed-input omission; existing HTTP transport tests still do not cover every resolver/error and subscription limit branch. | `U+T+E`: bearer extraction accepts only a non-blank case-insensitive Bearer value, supported principal forms normalize to one non-blank subject, reactive context retains the exchange and only valid causal watermarks, every catalog error preserves code/source/component/operation/requestId/errorId; depth/complexity, per-subject query, mutation, and subscription caps reject deterministically; cancellation releases admission; scalar invalid/null/overflow values are rejected without resolver execution. |
| QA10-B03 | `LiveUpdateFanout`, `BffEventConsumer`, `RabbitBffEventListener` | Unit fanout evidence exists, but duplicate events, consumer ack behavior, revocation, reconnect replay, and broker failure need separate boundaries. | `U+M+E`: duplicate event IDs produce one invalidation, unrelated groups/subjects receive nothing, membership removal terminates active subscriptions, malformed/poison/transient messages are acked/rejected/requeued according to policy, and reconnect requires explicit cursor recovery with no duplicate financial event. |
| QA10-B04 | `BrowserOriginPolicy`, `BrowserOriginWebFilter`, `BrowserCsrfWebFilter`, cookie/session filters | `BrowserOriginPolicyTest` explicitly covers absent origin, case normalization, default and non-default ports, malformed values, credentials, paths, queries, fragments, unsupported schemes, and wildcard rejection. In-process filter tests still do not prove the complete browser-cookie mutation chain. | `T+E`: allowed origin plus matching CSRF succeeds; missing/mismatched token, disallowed origin, unsafe method, access-cookie mutation, bearer-only native client, preflight, and WebSocket upgrade follow the documented policy; no cookie/token is leaked in logs or responses. |

### Expense Core financial, scheduling, and sync logic

| ID | Production target | Missing/weak evidence | Required acceptance criteria |
| --- | --- | --- | --- |
| QA10-C01 | `JpaExpenseStore` command/query methods | Report shows missed lines in the largest financial adapter; actor-scoped create/update/delete membership, participant, archived-group validation, duplicate-ID payload comparison, and injected outbox-failure rollback now have persistence assertions, but successful lifecycle E2E does not cover every transaction rollback branch. | `U+P+E`: create/update/delete/idempotent replay/tampered replay, stale version, missing/archived group, unauthorized actor, active membership identifiers, duplicate/inactive participants, conflicting duplicate-ID description/currency/payer/allocation payloads, audit row, sync revision, postings, balance, outbox, and rollback-after-each-write-failure are asserted; rejected validation leaves no revision, posting, idempotency, or expense row and the ledger remains zero-sum. |
| QA10-C02 | `ExpenseValidator`, `AllocationCalculator`, `FinancialArithmetic` | `AllocationCalculatorTest` now exercises permutation invariance, non-negative allocations, and minor-unit conservation across boundary totals for equal, weighted, and percentage modes. Broader property-based arithmetic evidence remains missing. | `U`: property/table tests cover zero, negative, maximum, overflow, fractional/unknown currency, duplicate participants, missing payer, percentages not summing to 100, exact minor-unit remainder distribution, deterministic ordering, and zero-sum preservation with reproducible seeds. |
| QA10-C03 | `RecurringExpenseService`, `RecurringExpenseController`, worker/claim locking | Coverage exists for basic lifecycle but not all date, catch-up, lock, and generated-expense failure branches. `RecurringExpenseServiceTest` exercises create/update missing or cross-group outcomes, validation guards, and both reachable one-sided custom-specification paths without changing production logic. | `U+P+E`: weekly/monthly/month-end clamp/timezone/end-date policy, pause/resume idempotency, missing/cross-group schedule, concurrent worker claims, bounded catch-up, deterministic occurrence IDs, duplicate prevention, failed occurrence rollback, and outbox/sync effects are asserted. |
| QA10-C04 | `JpaGroupStore`, invite/member lifecycle | Existing journey covers the happy path but not every replay/expiry/revocation and authorization transition. | `U+P+E`: invite expiry boundary, revoked/claimed/unknown token, simultaneous claim, placeholder binding, member removal, removed-member token/session, archived group, revision/audit/outbox effects, and non-member indistinguishable not-found behavior are asserted. |
| QA10-C05 | `JpaSettlementStore`, `SettlementService`, balances/suggestions/reconciliation | Settlement success, replay, and PostgreSQL posting reconciliation are covered; service-level validation and nullable suggestion delegation are now specified, while independent corruption detection and rollback evidence remain required. | `U+P+E`: valid settlement/reversal, same participant, negative/non-numeric/overflow amount, missing/cross-group settlement, authenticated idempotency identity and conflict, blank/invalid idempotency inputs, blank reversal reason, nullable suggestion engine, concurrent reversal, balance recomputation from postings, intentional corruption detection, and zero-sum invariant are asserted. |
| QA10-C06 | `JpaSynchronizationStore`, sync controllers/cursors | Current E2E proves common cursor recovery, not all cursor ownership and transaction boundaries. | `U+P+E`: snapshot/change ordering, empty page, first/last page, limit bounds, malformed/expired/decreasing cursor, cross-group cursor, tombstone, membership loss, concurrent revision allocation, and no revision advance after rejected mutation are asserted. |

### Notifications and event delivery

| ID | Production target | Missing/weak evidence | Required acceptance criteria |
| --- | --- | --- | --- |
| QA10-D01 | `AuthEmailRabbitListener`, `AuthEmailDeliveryConsumer`, envelope parser | Parser/ack branches have partial coverage; listener behavior depends on RabbitMQ channel semantics. | `U+M+E`: valid encrypted envelope is consumed once; missing/wrong-type/malformed/expired payload is rejected without requeue; transient SMTP/decrypt failure requeues once then parks; duplicate event is acknowledged without a second email; no credential appears in logs or DLQ payloads. |
| QA10-D02 | `RabbitNotificationListener`, `NotificationEventConsumer`, `TransactionalNotificationEventProcessor`, `RedisDeliveryRateLimiter`, broker parser | Existing persistence tests cover applied/duplicate/concurrent inbox outcomes; `NotificationEventConsumerTest` now also rejects blank and overlength subject, event-type, and message fields before either inbox or processed-event persistence. `NotificationEventConsumerUnitTest` specifies recipient trimming/validation, preference/rate suppression, durable constraint-duplicate acknowledgement, non-duplicate constraint propagation, preference/dispatcher failure isolation, and broker-retry propagation. Explicit transaction/ack coupling, poison/retry matrix, and production Redis admission evidence remain open. `RedisDeliveryRateLimiterTest` covers atomic key/arguments, allow/deny, null decisions, and Redis failure propagation. | `U+M+E`: applied, duplicate, malformed, poison, transient, and permanent failures produce exact DB transaction and manual ack/reject/requeue outcome; Redis admission uses the subject-scoped key, configured TTL/limit, atomic increment, and fail-closed store behavior; inbox, processed-event, preference, and email side effects commit together or roll back together. |
| QA10-D03 | `SmtpMailSender`/mail adapter, `EmailDispatcher`, `SimpleMailMessage` | Adapter line coverage was low; `SmtpJavaMailSenderTest` exercises the local SMTP wire sequence, and `SimpleMailMessageTest` covers array-content equality, nullable accessors, hash code, and string representation. External Mailpit/SMTP failure evidence remains open. | `U+M+E`: valid message maps recipient/template/body correctly; invalid recipient and missing preference are suppressed; SMTP timeout/auth/rejection is classified as retryable/permanent; retry count, DLQ/parking, metrics, and redaction are asserted. |
| QA10-D04 | Notification inbox/preferences stores/controllers | `JpaPreferenceStoreTest` proves blank and overlength preference subjects are rejected before persistence. `InboxControllerTest` proves blank mark-read subjects and valid cursors beyond stored data; `JpaNotificationInboxStoreTest` proves blank/overlength subjects and blank event/message payloads are rejected before any row is created. | `T+P+E`: subject isolation, default preferences, update versioning, blank/overlength subject rejection before query, invalid page/cursor bounds, valid cursor exhaustion, unknown notification, duplicate mark-read, missing membership/context, reader fallback, and database failure map to exact public errors without changing another subject’s rows. |

### Shared libraries and cross-cutting infrastructure

| ID | Production target | Missing/weak evidence | Required acceptance criteria |
| --- | --- | --- | --- |
| QA10-E01 | `GlobalErrorHandler`, `RequestIdFilter`, `RequestIdContext` | The available baseline has strong unit evidence for catalog/framework mappings, but it is not a fresh current-source report; deployed framework wiring, redaction, and transport serialization are not proven by these unit tests. | `U+T`: every catalog error, framework validation, malformed body, missing binding, type mismatch, media negotiation, optimistic conflict, unexpected exception, and rate-limit response asserts status, content type, stable code, source, request ID, bounded detail, and `Retry-After`; valid/invalid/oversized request IDs are propagated or replaced and MDC is cleared. |
| QA10-E02 | `DbAutoConfiguration`, `DbRoutingDataSource`, `DbOperationPolicy`, reader health/lag | Focused policy, pool-bound, reader-health, and scheduler tests exist, but the available JaCoCo baseline and exact current-source mapping require regeneration; configuration wiring, live datasource failure, and replica behavior still require persistence/deployed evidence. | `U+P+E`: writer/reader route policy, command/strong/eventual query classification, fallback on lag/disconnect, recovery, causal watermark capture/validation, Flyway writer datasource, bounded pool acquisition, scheduler lifecycle, and no reader use for writes/locks/claims are asserted. |
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

## QA-10 completion checklist

QA-10 may move to `done` only when every item below has a recorded command,
artifact, source revision, and reviewer in `docs/tasks/progress.md`:

1. Regenerate all application/library JaCoCo reports from the exact revision
   under review.
2. Run `report_branch_gaps.py --format json --fail-on-gaps`; it exits zero and
   the report contains no unclassified production branch method.
3. Execute the focused U/T/P/M tests for every non-zero QA10-A01..E05 row,
   asserting the row's invariant, failure behavior, and durable side effects.
4. Reconcile all 54 REST/GraphQL operations against the operation matrix and
   attach signed-persona authorization, negative-path, replay, pagination,
   concurrency, and side-effect evidence to QA10-E2E01.
5. Execute QA10-E2E02..E2E05 on the real OIDC/Compose topology, preserving
   broker, Mailpit, Redis, WebSocket, replica, and causal-read artifacts.
6. Execute QA10-E2E06 and QA10-E2E07 in the approved production-like
   environment, preserving threshold, failover, restore, rotation, scanning,
   rollback, alert, RPO/RTO, and reconciliation artifacts.
7. Re-run contract, strict Python typing, security/architecture, diff, and
   repository test gates; record unavailable checks as unavailable rather than
   inferred passes.

The hosted Gradle workflow produces JaCoCo reports in a per-module matrix.
The `qa10-coverage-inventory` job now downloads those module artifacts,
restores them into their repository paths, and publishes one aggregate JSON
inventory. It is intentionally discovery-only while the baseline contains
150 gaps; `--fail-on-gaps` remains the eventual blocking closure step. A
single matrix shard is insufficient evidence for a repository-wide
no-missed-branch claim.

## Closure evidence required per increment

Each increment must record exact Gradle/Python/Compose commands, test counts,
artifact paths, source revision, and limitations in `docs/tasks/progress.md`.
The relevant operation-matrix rows and task detail must be updated in the same
increment. A failed or unavailable Docker/hosted run is recorded as unrun or
blocked; it is never converted into a mocked pass. JaCoCo must be regenerated
after tests are added, and every remaining missed class must be classified as
behavioral gap, infrastructure gap, or generated/structural code with a reason.
