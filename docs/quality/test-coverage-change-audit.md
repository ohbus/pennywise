# Test-coverage change audit

**Scope:** every branch commit from merge-base `65b2fb66d61d90b4d92e4e23a55e06f43cd89270`
(`master`) through the current audited branch tip, whose subject indicates
tests, coverage, QA, JaCoCo, or E2E work, plus the restoration and CI commits
that changed the audit evidence or test execution boundary.

## Rule applied

Coverage improvements must come from executable unit, integration, messaging, or
E2E tests and their acceptance evidence. Existing implementation logic and
public contracts are preserved. A behavior change is acceptable only when an
observed defect or an explicit requirement justifies it, with a focused test,
documentation, and a separately reviewable commit.

## Audit result

The audit found no contract-file changes in the test/coverage/QA commit set.
Most commits were test, tooling, or documentation-only. Four commits had
production implementation edits:

## Current residual review increment

The current regenerated ledger keeps all 39 methods and 78 missed JaCoCo
branches visible. It reclassifies the nine `JpaGroupStore` records as
`BEHAVIOR-COVERED-MAPPING` after reviewing `JpaGroupStoreTest` and
`JpaGroupStoreClaimTest`: normal and rejection lifecycle paths, archived and
missing groups, placeholder/invite validation, duplicate removal, expiry,
claim races, rollback, revisions, audit, synchronization, and outbox effects
are exercised without changing the store. The residual JaCoCo counters remain
in the ledger and are treated as line/instrumentation mappings, not removed
coverage gaps. The resulting review split is 8 candidate structural mappings,
30 behavior-covered mappings, and one open-design helper. No implementation,
contract, guard, or JaCoCo exclusion was added or removed.

The same review covers the five `RecurringExpenseService` records through
`RecurringExpenseServiceTest`, `RecurringExpenseFailureTest`, and
`RecurringExpenseOptionalOutboxTest`. These tests exercise optional custom
participants, catch-up/end-date/paused/duplicate paths, membership and
generation failures, durable schedule/expense state, and notification behavior
with and without an outbox. The residual JaCoCo lines remain visible as tested
service-boundary mappings; no fallback, guard, or contract was removed.

The residual `GlobalErrorHandler.applicationException` mapping is also
behavior-covered by `GlobalErrorHandlerTest`: every catalog code and governed
HTTP status, invalid mocked status fallback, null-message fallback,
`Retry-After`, headers, correlation, negotiation, and redaction are asserted.
The exhaustive mapping and fail-safe fallback remain in production; only the
JaCoCo mapping is classified.

The follow-up review also classifies the residual `FallbackJwtDecoder`,
`SettlementSuggestionEngine`, `DbReaderHealth`, and `DbOperationPolicy` records
as behavior-covered mappings. Existing tests cover decoder ordering and failure,
settlement conservation and isolation, reader state transitions and deadlines,
and route-policy validation. No decoder, settlement, database policy, or health
implementation was deleted or weakened.

The same test-backed review covers `EmailAddress`, `SessionPolicy`, and
`ClientAddressResolver`. Their canonicalization, length/IDN/control/label,
expiry/skew, trusted-proxy, IPv4/IPv6, malformed, and missing-address cases
pass through the existing boundary suites. Parser guards, expiry rules, and
proxy trust behavior remain unchanged; only residual JaCoCo mappings are
classified.

The follow-up also covers `BrowserOriginPolicy` and `RecurrenceSchedule` through
their existing tests. Exact-origin/default-port/native-only/malformed/wildcard
cases and schedule identity/day/frequency cases pass without changing the
fail-closed guards or recurrence contract.

The search, sync, and notification follow-up similarly classifies only residual
test-backed mappings. Search tests cover authorization, filtering, pagination,
cursor/limit validation, CSV safety, and reader routing; sync tests cover
membership and cursor failure modes; email tests cover delivery, retry,
interruption, and permanent failure outcomes. No implementation or public
contract was deleted or weakened.

As a reproducible history check, the subject-matching audit selected **240
commits** from `master..HEAD` whose subjects contain `test`, `coverage`, `QA`,
`JaCoCo`, or `E2E`. `git diff-tree --diff-filter=D` found **no file deletion**
in that set, and the same commit set has **no changes under `contracts/`**.
This check is intentionally separate from the implementation-diff review below:
an implementation modification is not silently treated as a test-only change,
and a lower coverage number is never accepted as evidence by itself.

| Commit | Finding | Disposition |
| --- | --- | --- |
| `aeb31e5` | Removed explicit email domain/label validation and simplified the JWT fallback terminal state while closing branch gaps. | Restored. Tests remain; JaCoCo must measure the implementation that ships. |
| `19d7a1b` | Added blank-message fallbacks after tests exposed unsafe response detail behavior, but also removed the explicit `ErrorCode` HTTP-status fallback mapping. | Blank-message behavior retained as a tested defect correction; status fallback restored. |
| `a52b467` | Changed the login abuse window boundary from exclusive to inclusive at exact expiry. | Retained as an evidenced behavior correction: the acceptance invariant is that a request at expiry is allowed. |
| `fc42e27` | Removed an unused private profile problem mapper in a test-coverage commit. | Restored. Any dead-code cleanup requires a separate justification and review. |

The following test/coverage/QA commits were implementation-preserving or
documentation/tooling-only: `13a441c`, `1d7f028`, `664d504`, `f19d3da`,
`a6812eb`, `0ed2e7d`, `5275496`, `3ca0573`, `d4e28fd`, `d1beb19`,
`2118925`, `ecc0ba5`, `c9464c6`, `ea56495`, `176427c`, `9983199`,
`89b3371`, and `dfa25ae`.

## Audit of the post-`dfa25ae` branch increments

The branch was re-audited from `dfa25ae` through `c4b8852`; the merge-base is
still `65b2fb6`. `2110ffd` is the explicit restoration/audit commit: it restored
the email-domain checks, JWT fallback terminal guard, profile mapper, and error
status fallback that had been removed in earlier coverage work. It is not a
coverage-driven deletion. `3b3a3da` changes only `libs/security/build.gradle.kts`
to provide test-scope web/converter dependencies for decoder tests; it does not
alter production implementation or runtime dependency behavior. `cff7f76`
changes CI/Makefile Python execution to immutable, frozen, no-build tooling.

The remaining post-`dfa25ae` commits through the previous audit tip are implementation-preserving tests or
documentation/tooling increments:

`c133f69`, `da69b8f`, `7be34df`, `b73792c`, `9f283ef`, `56a41c5`, `e643cd5`,
`9598938`, `e3861a9`, `3aa0257`, `83e788a`, `49fe921`, `d907f6c`, `a3e806a`,
`2be84c5`, `76becf4`, `c55e6b0`, `7acb986`, `0597559`, `cb73ac9`, `880fd3c`,
`cf4533f`, `af67c75`, `f096886`, `abff7ab`, `9b2fa85`, `dbac7e7`,
`8034208`, `bb35873`, `52f270c`, `6ef0803`, and `c4b8852`.

Since that audit checkpoint, `73e111c` fixes a missing JDK exception import in
a BFF test, `69f2d27` adds persistence-backed recurring-expense tests plus the
synchronized QA-10 baseline, `4b391a6` adds notification payload-boundary
tests, `cb02a39` adds in-place expense-update persistence coverage, and
`811bcdd` adds positive recurring-update coverage. These increments preserve
production behavior and public contracts; no implementation file was deleted
in any of them. `2a532eb` adds duplicate-expense payload conflict coverage,
and `0e66e36` adds invitation expiry and claim-replay coverage. `7d8e316` adds
transport validation coverage for invalid expense-update financial inputs. All
three are implementation-preserving tests; no production implementation or
contract file changed. `95390e8` adds settlement replay and group-state
coverage, including no-posting guarantees for rejected financial mutations.
`ce406b6` adds create-side expense financial-input validation coverage; it also
preserves the existing `ERR_02` contract and changes no production code.

`7ee42a1` adds notification-envelope subject/message fallback coverage without
changing the broker conversion contract or production implementation.

`0f79f86` adds persistence-backed delete boundary coverage for missing groups,
missing expenses, null-version deletion, repeated deletion, revision stability,
and immutable ledger net-zero behavior. It changes no production implementation
or public contract; the regenerated report removes `JpaExpenseStore.delete`
from the open branch inventory and reduces the baseline from 148 methods / 363
missed branches to 147 methods / 356 missed branches.

`05c9417` adds persistence-backed and repository-boundary claim coverage for
archived groups, removed or already-bound placeholders, concurrent targeted
claims, and missing group/placeholder rows. It changes no production
implementation or public contract; the regenerated report reduces missed
branches from 356 to 351 while retaining the four residual `claim` branches
for explicit JaCoCo mapping/reachability review.

`48fe0db` adds persistence-backed update coverage for missing groups, missing
expenses, soft-deleted expenses, and payer/allocation participant replacement.
It changes no production implementation or public contract; the regenerated
report reduces missed branches from 351 to 346 and leaves one update mapping
branch open for review.

`0e267b7` adds a complete GraphQL resolver classification matrix for all
catalog error codes, upstream status mappings, argument failures, unknown
failures, safe details, classifications, request IDs, and rate-limit metadata.
It changes no production implementation or GraphQL contract; the regenerated
report removes all three resolver records and reduces missed branches from 346
to 335.

`6b267bf` adds persistence-backed recurring-schedule creation tests for
explicit schedule identifiers and valid lower/upper day-of-month boundaries,
including rejection above the supported range. It changes no production
implementation or contract; the regenerated report retains 144 records and
reduces missed branches from 335 to 331. One compiler-generated range branch
remains explicitly open for review.

`fa41e93` changes only reusable-CI RabbitMQ health-check timing and the
corresponding CI documentation/progress entry. It does not alter production
implementation, contracts, or coverage evidence.

`5007283` adds public REST boundary tests for missing and blank settlement
principal subjects. It changes no production implementation or contract; the
regenerated report removes `SettlementController.ensureMembership` and reduces
the inventory from 144 records / 331 missed branches to 143 records / 326
missed branches.

`01f5513` adds in-memory outbox retry-policy tests for invalid max-attempt and
negative-delay inputs plus unknown-event no-op behavior. It changes no
production implementation or contract; the regenerated report removes
`OutboxRelay.reject` and reduces the inventory from 143 records / 326 missed
branches to 142 records / 321 missed branches.

`9422d9a` adds PostgreSQL-backed outbox retry-policy tests for the same invalid
inputs and unknown-event no-op contract. It changes no production
implementation or contract; the regenerated report removes
`JpaOutboxStore.reject` and reduces the inventory from 142 records / 321 missed
branches to 141 records / 318 missed branches.

`6f2d5f9` adds in-memory outbox state-boundary tests for duplicate IDs,
invalid claim limits, missing claim leases, and unknown acknowledgements. It
changes no production implementation or contract; the regenerated report
removes all remaining `OutboxRelay` records and reduces the inventory from
141 records / 318 missed branches to 137 records / 310 missed branches.

`9895812` adds constructor-policy tests for `OutboxPublisher`, covering invalid
batch size, lease, and maximum-attempt values. It changes no production
implementation or contract; the regenerated report removes the publisher from
the open inventory and reduces the baseline from 137 records / 310 missed
branches to 136 records / 303 missed branches.

`4145a05` adds PostgreSQL-backed claim-boundary tests for `JpaOutboxStore`,
covering invalid claim policies, future pending messages, and active leases.
It changes no production implementation or contract; the regenerated report
removes `JpaOutboxStore.claim` from the open inventory and reduces the baseline
from 136 records / 303 missed branches to 135 records / 298 missed branches.

`eb045bb` adds public controller tests for blank-category defaulting on create
and update. It changes no production implementation or contract; the
regenerated report reduces missed branches from 298 to 290 while retaining the
null-principal forwarding branch for structural review.

`8b26630` adds a public controller test for the allocation-side participant
limit, independent of payer count. It changes no production implementation or
contract; the regenerated report reduces missed branches from 290 to 289.

`c5b924c` adds recurring-controller coverage for custom payer/allocation request
mapping and missing/foreign schedule lookup, pause, and resume boundaries. It
changes no production implementation or contract; the regenerated report
removes four recurring-controller records and reduces the baseline from 135
records / 289 missed branches to 131 records / 281 missed branches.

`9692227` adds recurring-controller tests for malformed and non-positive amounts,
blank authenticated subjects, and non-member subjects. It changes no production
implementation or contract; the regenerated report removes two recurring
controller records and reduces the baseline from 131 records / 281 missed
branches to 129 records / 276 missed branches.

`b60f458` adds malformed member-removal payload coverage to the BFF event
consumer. It changes no production implementation or public contract; the
regenerated report removes `BffEventConsumer.consume` from the open inventory
and reduces the baseline from 129 methods / 276 missed branches to 128 methods
/ 271 missed branches.

`ff5fac3` adds public fanout boundary, queue, input, and expiry tests. It changes
no production implementation or public contract; the regenerated report removes
five `LiveUpdateFanout` records and reduces the baseline from 128 methods /
271 missed branches to 123 methods / 254 missed branches. Three generated
revocation/expiry lambda records remain for explicit structural or boundary
review; no implementation was deleted to improve the metric.

`9b3e19e` adds persistence-backed recurring update coverage for allocation-only
and payer-only custom specifications, asserting the omitted side is derived when
occurrences are generated. It changes no production implementation or public
contract; the regenerated report reduces missed branches from 254 to 251 while
retaining the one generated `updateSchedule` mapping branch for review.

`080a970` adds settlement suggestion coverage for duplicate participant/currency
balance rows, proving aggregation before greedy matching. It changes no
production implementation or public contract; the regenerated report reduces
missed branches from 251 to 250 while retaining three engine mappings for
reachability review.

`cb41222` adds fail-closed auth-email envelope tests for invalid AES key sizes,
truncated framing, and unsupported versions. It changes no production
implementation or public contract; the regenerated report removes the envelope
constructor record and reduces missed branches from 250 to 245 while retaining
one `reveal` authentication mapping branch for exact instrumentation review.

`47603a8` adds notification-envelope conversion tests for blank selected subject
and message fields, asserting the existing safe group/event defaults. It changes
no production implementation or public contract; the regenerated report removes
`toNotificationEvent` from the open inventory and reduces missed branches from
245 to 241.

`53ad6ed` adds malformed `schemaVersion` and `groupRevision` cases for the
notification broker envelope parser, covering missing, text, and fractional
numeric metadata. It changes no production implementation or public contract;
the regenerated report removes both numeric helper records and reduces missed
branches from 241 to 235.

`3a98e20` adds auth-email configuration tests for valid exact-length keys and
malformed or incorrectly sized Base64 values. It changes no production
implementation or public contract; the regenerated report removes the
configuration method record and reduces missed branches from 235 to 232.

`5e4a0a4` adds auth-email listener tests for missing required envelope fields
and channel-optional successful delivery. It changes no production
implementation or public contract; the regenerated report preserves the 118
method inventory and reduces missed branches from 232 to 229. Broker-level
retry/redelivery and deployed Mailpit evidence remain open.

`a399b2a` adds SMTP adapter tests for configured sender/optional-field
fallbacks and premature response-stream rejection. It changes no production
implementation or public contract; the regenerated report removes both SMTP
adapter records and reduces the inventory from 118 methods / 229 missed
branches to 116 methods / 225 missed branches. Mailpit integration and
deployed retry/redaction evidence remain open.

`6adca2d` adds dispatcher tests for interrupted retry delays, nested permanent
causes, direct I/O retry classification, and overlength recipients. It changes
no production implementation or public contract; the regenerated report
removes the `isValidEmail` record and reduces the inventory from 116 methods /
225 missed branches to 115 methods / 223 missed branches. The dispatcher retry
loop still has two mapped residual branches, and Mailpit integration remains
open.

`7887a41` adds broker-envelope parser tests for missing and non-string required
metadata and for an absent payload object. These cases assert the parser’s
fail-closed `InvalidEnvelopeException` contract without changing production
logic. The focused Notifications suite and full Java 25 wrapper run passed; the
regenerated inventory falls from 82 records / 150 missed branches to 81 records
/ 148 missed branches. Broker redelivery and deployed notification evidence
remain open.

`13f6c30` adds GraphQL DateTime literal acceptance and rejection tests. It
changes no production implementation or GraphQL contract; the regenerated
report preserves 115 method records and reduces missed branches from 223 to
221. The remaining nullable-literal mappings are retained for structural
JaCoCo review, and deployed GraphQL transport evidence remains open.

`35c0242` adds a persistence-backed empty synchronization snapshot test,
asserting no changes, no continuation cursor, and `hasMore=false`. It changes
no production implementation or contract; the regenerated report preserves
115 method records and reduces missed branches from 221 to 220. Cursor
validation, causal revision, and deployed synchronization evidence remain
open.

`c4d3553` adds unit coverage for normal and targeted invitation claim races
where the atomic repository update returns zero. It changes no production
implementation or contract; the regenerated report preserves 115 method
records and reduces missed branches from 220 to 219. Three claim mappings,
concurrency artifacts, and deployed group-lifecycle evidence remain open.

`dcdc2c2` adds a persistence-backed search test for legacy category fallback and
non-null opaque cursor handling. It changes no production implementation or
contract; the regenerated report preserves 115 method records and reduces
missed branches from 219 to 218. Search authorization, pagination isolation,
export, and deployed financial E2E evidence remain open.

`7a9689f` adds a JPA invitation-boundary test for an active ordinary membership
supplied as a placeholder target. It changes no production implementation or
contract; the regenerated report preserves 115 method records and reduces
missed branches from 218 to 217. Invitation concurrency, mutation side effects,
and deployed group-lifecycle evidence remain open.

`433ebe2` adds listener coverage for an unparsable auth-email expiry. It changes
no production implementation or contract; the regenerated report preserves 115
method records and reduces missed branches from 217 to 216. Broker acknowledgement,
retry/DLQ, redaction, and deployed Mailpit evidence remain open.

`5592be7` adds expense-controller coverage for explicit-null category defaulting
and simultaneous oversized payer/allocation collections. It changes no production
implementation or contract; the regenerated report preserves 115 method records
and reduces missed branches from 216 to 215. Financial mutation rollback and
deployed lifecycle evidence remain open.

`b5b814c` adds a Spring integration test for recurring expense-store failure.
It changes no production implementation or contract; the regenerated report
preserves 115 method records and 215 missed branches because the newly asserted
pause/error behavior was already represented by covered mappings. Remaining
recurrence date, membership, generated-expense fallback, and deployed worker
evidence remain open.

`0886354` adds public REST-boundary coverage for the optional settlement suggestion
engine fallback. It changes no production implementation or contract; the full
regenerated report reduces the inventory from 115 methods / 215 missed branches to
113 methods / 211 missed branches. Settlement authorization, rollback, concurrency,
corruption detection, and deployed financial E2E evidence remain open.

`a36b1c5` adds deterministic cursor-decoding tests for malformed structure, blank
group, revision, and expiry fields. It changes no production implementation or
contract; the full regenerated report reduces the inventory from 113 methods /
211 missed branches to 112 methods / 209 missed branches. Cursor ownership,
causal consistency, and deployed synchronization E2E evidence remain open.

`5bfaf7e` adds persistence coverage for rejecting an invitation targeted at a
placeholder removed after creation. It changes no production implementation or
contract; the full regenerated report preserves 112 method records and reduces
missed branches from 209 to 208. Invitation concurrency, audit/outbox effects,
and deployed group-lifecycle E2E evidence remain open.

`b5c9abd` adds persistence integration coverage for both sides of the
`JpaSearchStore` export-limit guard. It changes no production implementation or
contract; the full regenerated report reduces the inventory from 112 methods /
208 missed branches to 111 methods / 205 missed branches. Search authorization,
cursor isolation, export behavior, and deployed financial E2E evidence remain open.

`b84502b` adds persistence integration coverage for durable outbox append: a
duplicate event identifier fails closed and cannot replace the original payload.
It changes no production implementation or contract; the full regenerated report
reduces the inventory from 109 methods / 199 missed branches to 108 methods /
197 missed branches. Outbox broker delivery, transaction rollback, and deployed
messaging evidence remain open.

`80c3f5d` adds persistence coverage for an email update targeting an unmapped
account, asserting the existing fail-closed exception before any mutation. It
changes no production implementation or contract; the full regenerated report
preserves 108 method records and reduces missed branches from 197 to 196. The
identity table's profile foreign key makes profile-missing identity rows
unreachable through the real persistence boundary; concurrent identity races and
deployed identity evidence remain open.

`22a6be3` adds an explicit BFF configuration test for malformed allow-listed
origins, proving deployment fails fast instead of accepting an invalid browser
origin. It changes no production implementation or contract; the regenerated
inventory remains 108 method records / 196 missed branches because the malformed
origin path was already represented in JaCoCo coverage.

`dce224a` adds BFF reactive-boundary coverage for a bearer scheme containing only
whitespace, asserting no blank token enters the Reactor context. It changes no
production implementation or contract; the regenerated report reduces the
inventory from 108 methods / 196 missed branches to 107 methods / 194 missed
branches. Upstream authorization, watermark propagation, and deployed GraphQL
evidence remain open.

`bcb5c8a` adds GraphQL controller coverage for an empty `updateGroup` gateway
response, asserting no fabricated invalidation is published. It changes no
production implementation or contract; the regenerated report reduces the
inventory from 107 methods / 194 missed branches to 106 methods / 193 missed
branches. Upstream mutation authorization, replay, and deployed GraphQL evidence
remain open.

`eb5b6f5` adds equivalent empty-response coverage for GraphQL `createExpense`
and `recordRepayment`, asserting neither publishes a fabricated invalidation. It
changes no production implementation or contract; the regenerated report reduces
the inventory from 106 methods / 193 missed branches to 104 methods / 191 missed
branches. Upstream mutation authorization, replay, and deployed GraphQL evidence
remain open.

`efbc4b9` adds BFF configuration tests proving enabled RabbitMQ fanout is
accepted and disabled fanout fails startup validation in deployed/local-OIDC
profiles. It changes no production implementation or contract; the regenerated
report reduces the inventory from 104 methods / 191 missed branches to 103
methods / 189 missed branches. Broker connectivity and deployed profile evidence
remain open.

`7f184e4` adds BFF deduplicator capacity-boundary coverage for zero and negative
limits, preserving the bounded-cache invariant. It changes no production
implementation or contract; the regenerated report reduces the inventory from
103 methods / 189 missed branches to 102 methods / 187 missed branches. Broker
replay and deployed fanout evidence remain open.

The current branch tip was checked separately: the committed changes contain
test, tooling, CI, and documentation changes, with no production implementation
or contract-file change in the current coverage increment. No branch in this audit is
closed by deleting implementation logic, weakening a guard, changing a public
contract, or adding a coverage exclusion.

## Current evidence

After restoring implementation logic and retaining the added tests, the freshly
regenerated reports contain 102 methods with 187 missed branches. The remaining
count is an honest discovery baseline, not a claim that any implementation was
removed to improve metrics. The full repository Gradle test and JaCoCo run and
all four application test suites pass under Java 25; environment-owned E2E
evidence remains open. The latest Accounts increment adds a PEM-backed RSA key-loading
test without changing production code; `DefaultRsaKeyProvider.loadOrGenerateKey`
now reports 8 covered and 4 missed branches, with the residual method retained
for further test or structural review. The subsequent session-persistence
increment adds expiry, missing-identity, and deletion-request outcomes without
changing production code; total missed branches are now 746, and
`TokenSessionService.rotateSession` reports 22 covered and 4 missed branches.
The follow-on logout ownership increment preserves the no-op behavior for blank,
unknown, and missing-identity tokens while proving matching-subject family
revocation; total missed branches are now 743 and
`revokeSessionByRefreshToken` reports 11 covered and 1 missed branch.
The cryptographic configuration increment adds direct valid, malformed, and
length-boundary tests without changing production code; total missed branches
are now 737, and both `decodeSecret` and `decodeEnvelopeKey` are absent from
the branch-gap inventory.
The follow-on AuthController transport increment adds explicit CODE selection,
missing-channel LINK defaulting, and blank-principal rejection without changing
production code; total missed branches are now 733 and the AuthController
branch gaps are closed locally.

The identity persistence increment adds profile fallback, missing/unknown lookup,
nullable-email, and re-enrollment tests without changing production code. Its
focused suite passed, but the full JaCoCo report could not be regenerated after
the wrapper distribution cache stopped being usable; therefore the 188-method
baseline is intentionally unchanged and no coverage reduction is claimed.

The subsequent login-start increment adds `LoginStartServiceTest` for both
delivery templates, generic credential-issuance failure, ordinary rate-limit
denial, and rate-limit-store outage. It changes no production behavior; the
focused test and JaCoCo effect remain unverified until Gradle 9.7.1 can run.

The QA10-C03 increment adds a real persistence-backed test for the two
reachable one-sided recurring-expense specification paths. Payer-only schedules
retain their explicit payer and receive equal allocations; allocation-only
schedules retain their explicit allocations and derive the first active member
as payer. The fresh report reduced missed branches from 434 to 419 without
deleting or weakening implementation logic. Worker concurrency, rollback,
outbox, and deployed recurrence evidence remain open.

The QA10-D02 increment adds persistence-backed invalid-boundary tests
for `TransactionalNotificationEventProcessor.toInboxEntity` through the public
consumer. Blank and overlength subject, event-type, and message values are
rejected before either durable inbox or processed-event state is written. The
fresh report removed that method from the inventory and reduced missed branches
from 419 to 407 without changing notification implementation logic or contracts.

The QA10-C01 persistence increment adds an in-place update test for an
existing payer and allocation participant. It asserts the version and payload
change while preserving one payer/allocation row per participant and a zero net
posting balance. The fresh report reduces the `JpaExpenseStore.update` residual
from 12 to 6 missed branches and the repository total from 407 to 402, with no
production implementation or contract change.

The follow-up QA10-C03 increment adds a valid positive `updateSchedule` case
covering monthly day-of-month, end-date, payer, and allocation combinations.
The test asserts the persisted schedule fields; JaCoCo reduces that method's
residual from 13 to 4 missed branches and the repository total from 402 to 393.

The subsequent QA10-C01 increment adds duplicate-ID conflict coverage for every
payload dimension compared by `JpaExpenseStore`: description, currency, payer
identity, and allocation identity. Each conflicting replay returns `ERR_06`,
leaves the original persisted payload unchanged, and preserves the two original
balance postings. This test changes no implementation logic or public contract;
the regenerated report reduces the repository total from 393 to 383 missed
branches.

The subsequent QA10-C01 controller increment covers malformed and non-positive
payer amounts, payer-currency mismatch, payer-total mismatch, and invalid exact
allocation before the expense store is called. The test preserves the existing
`ERR_02` transport contract; the regenerated report reduces the repository total
from 381 to 377 missed branches.

The latest QA10-C01 controller increment mirrors the update validation matrix
on `createExpense`: malformed/non-positive payer amounts, currency mismatch,
sum mismatch, and invalid exact allocation are rejected before persistence.
The regenerated report reduces the current total from 369 to 366 missed
branches.

The notification conversion increment then reduces the current total from 366
to 363 missed branches while preserving the existing fallback order.

The subsequent QA10-C01 invitation increment covers an expired invitation,
same-subject claim idempotency through a second invitation, and a competing
subject attempting an already-claimed token. The test preserves the existing
conflict semantics and verifies no duplicate membership or revision side effect;
the regenerated report reduces the repository total from 383 to 381 missed
branches.

The audit-control increment makes closure requirements executable in the
documentation tooling: every branch record must name its exact test and
invariant, and every operation must account for authentication, authorization,
failure, durable/asynchronous state, replay/concurrency, isolation, and
redaction. This strengthens review accountability without excluding or deleting
any production branch.

The email-parser increment adds exact accepted length boundaries and invalid IDN
separator cases without changing `EmailAddress.parse`; its focused execution
and coverage effect remain pending the Gradle wrapper bootstrap.

The asymmetric-provider increment adds blank issuer and audience constructor
guard tests without changing token issuance or validation behavior; execution
and JaCoCo effect remain pending the Gradle wrapper bootstrap.

The login-verification increment adds an existing-identity redemption case that
asserts stable account, subject, and profile reuse without changing production
logic; execution and JaCoCo effect remain pending the Gradle wrapper bootstrap.

The deletion-store increment adds missing-record null-result and invalid-subject
integration assertions without changing deletion state transitions or profile
preservation behavior; execution and JaCoCo effect remain pending Gradle.

The A07 audit increment names all 11 current method-level records with source
lines, missed-branch counts, and required evidence/classification. A tooling
regression test protects that ledger; no branch was excluded or implementation
logic removed.

The follow-up audit-control test now compares the documented row counts and
operation names with the live branch and operation inventories, requiring all
41 no-E2E-signal operations to remain visible. The locked tooling suite passed
7 tests; no implementation or coverage exclusion changed.

The session-expiry review confirmed exact boundary tests already exist. The
remaining JaCoCo branch is retained for mapping review against the session
invariant; no duplicate test, code deletion, or exclusion was introduced.

The credential equality increment covers every custom equality branch, including
all fields, null, and unrelated types, without deleting the implementation or
excluding the value-object method; JaCoCo remapping remains pending.

The RSA rotation review confirmed existing tests cover the valid and guarded
rotation behavior, including overlap and successive generations. The residual
branch remains open for JaCoCo mapping and deployed rotation evidence; no
redundant test or implementation change was introduced.

The envelope review confirmed all reachable malformed, tampered, context, and
version paths are already tested. The remaining defensive crypto catch stays in
place for JaCoCo mapping review; no fail-closed handling was removed.

The refresh-race increment adds deterministic compare-and-set failure coverage
that asserts family revocation and no token minting. It does not claim to replace
the required real concurrent PostgreSQL/E2E race test.

The RSA key-loading increment adds partial-configuration fallback and malformed
complete-PEM assertions without changing generation or parsing logic; execution
and JaCoCo mapping remain pending the Gradle environment repair.

The settlement-service increment adds validation, authenticated replay identity,
conflict, reversal-reason, and absent-suggestion-engine tests without changing
settlement implementation or contracts. The focused JVM execution and JaCoCo
effect remain pending until the Gradle wrapper can bootstrap.

The structural-review increment documents three defensive JaCoCo candidates with
source invariants and retained tests. It does not add exclusions, delete code,
or classify the unreferenced profile mapper; that mapper remains open.

The notification-consumer increment adds direct unit evidence for recipient
normalization/validation, preference and rate-limit suppression, durable
constraint-duplicate acknowledgement, and propagation of non-duplicate failures.
It changes no consumer implementation, broker contract, or fail-closed policy;
real RabbitMQ and Redis integration evidence remains required.

The Expense Core increment adds JPA evidence for actor-scoped active membership
identifiers and duplicate/inactive participant rejection before any mutation.
It changes no ledger, authorization, or persistence implementation; injected
write-failure rollback and deployed financial lifecycle evidence remain open.

The follow-on Expense Core test adds non-member update/delete rejection and
asserts unchanged version, description, group revision, and posting count. It
preserves the existing object-authorization contract and does not alter deletion
or ledger behavior.

The archived-group increment adds JPA evidence that create, update, and delete
retain the `ERR_06` conflict contract and cannot mutate archived financial state.
It changes no archive, authorization, or ledger implementation.
`d9beab3` adds public REST-boundary coverage for query, currency, and category
filters on CSV expense export. It changes no production implementation or
contract; the full regenerated report reduces the inventory from 111 methods /
205 missed branches to 109 methods / 200 missed branches. Export authorization,
formula safety, durable search isolation, and deployed financial E2E evidence
remain open.
`7d1fbce` adds integration coverage for blank and unknown refresh-token rotation,
asserting both fail closed before a session lookup or token issuance. It changes
no production implementation or contract; the full regenerated report preserves
109 method records and reduces missed branches from 200 to 199. Real concurrent
rotation and deployed OIDC/session evidence remain open.
`b84502b` adds JPA coverage for duplicate durable outbox append, preserving the
first payload while rejecting replacement. The current total is 108 method
records and 196 missed branches; broker delivery and transaction rollback
evidence remain open.
`80c3f5d` adds JPA coverage for the unmapped-account email-update rejection. The
profile foreign-key invariant prevents meaningful orphan-identity fixtures, so
those residual branches remain explicitly classified as unreachable at the
persistence boundary rather than being tested with invalid database state.
`22a6be3` adds the malformed configured-origin construction assertion without
changing the current 108-record / 196-missed-branch baseline.
`dce224a` adds the blank-bearer context assertion and reduces the current total
to 106 method records / 193 missed branches after the empty update-response
increment in `bcb5c8a`.
`bcb5c8a` adds the empty `updateGroup` invalidation assertion without changing
the existing production or contract behavior.
`eb5b6f5` adds the matching empty `createExpense` and `recordRepayment`
invalidation assertions; the current total is 104 method records / 191 missed
branches.
`efbc4b9` adds required BFF messaging startup validation coverage; the current
total is 103 method records / 189 missed branches.
`7f184e4` adds the deduplicator capacity guard tests; the current total is 102
method records / 187 missed branches.

`6a2027e` adds valid and malformed listener tests with a nullable broker channel.
The listener consumes valid envelopes and tolerates poison-pill rejection when
the optional channel is unavailable, without changing implementation or broker
contracts. The current total is 101 method records / 185 missed branches;
deployed broker acknowledgement/requeue and WebSocket evidence remain open.

`a4c7f8f` adds BFF WebFilter boundary tests for the matching GraphQL fanout
acceptance fault and the ordinary pass-through path. No production implementation
or contract changed. The current total is 100 method records / 184 missed
branches; deployed GraphQL authentication, authorization, and WebSocket evidence
remain open.

`5fc7009` adds HTTP-double coverage for Expense Core gateway group, expense,
repayment, and settlement-suggestion response mapping plus typed mutation failure
propagation. No production implementation or contract changed. The current
total is 99 method records / 178 missed branches; timeout, partial-response, and
deployed gateway evidence remain open.

`14eeb9c` adds causal-watermark boundary assertions for absent downstream
values, preserving the current watermark or returning null when no value exists.
No production implementation or contract changed. The JaCoCo inventory remains
89 method records / 168 missed branches because the residual branch is a Kotlin
short-circuit mapping candidate; the explicit causal-header acceptance behavior
is now covered.

`bf15f07` adds an HTTP-double slice exercising all nullable-bearer gateway
operations and asserting that absent credentials produce no Authorization header.
No production implementation or contract changed. The current total is 89
method records / 168 missed branches; timeout, partial-response, malformed-body,
and deployed gateway evidence remain open.

`1974d8f` adds the BFF settlement output fallback assertion: a missing
`amountMinor` is exposed as the documented zero minor-unit string. No production
implementation or contract changed. The current total is 88 method records /
167 missed branches; remaining BFF scalar/mapping residuals require reachability
review rather than implementation deletion.

`d661fb8` adds GraphQL subscription admission tests for blank group IDs and
missing authenticated subjects, asserting exact `ERR_02` and `ERR_03` failures
before a live-update slot is admitted. No production implementation or contract
changed. The current total is 87 method records / 165 missed branches.

`f0a7946` adds the missing recurring membership direction: a custom payer who
is not a group member now causes the schedule to pause with
`invalid_membership`, creates no occurrence, and leaves the existing production
membership guard unchanged. The focused and full Gradle wrapper suites passed;
the regenerated total is 87 method records / 163 missed branches. Worker
concurrency, rollback, broker, and deployed recurrence evidence remain open.

`dfe7661` adds deletion-store tests for missing cancellation/completion records
and for persisted `CANCELLED`/`COMPLETED` transitions. The focused Accounts
test and full repository JaCoCo run passed; the deletion-store records are no
longer present in the current 87-record inventory. The aggregate remains 163
missed branches because other residual mappings remain open. No production
logic or deletion contract changed.

`7418acc` adds cleanup boundary tests for an empty expired-claim page and all
non-positive retention/batch constructor guards. The focused Expense Core test
and full repository JaCoCo run passed; the regenerated inventory falls from
87 records / 163 missed branches to 86 records / 158 missed branches. Durable
multi-worker cleanup and operational scheduling evidence remain open. No
production implementation or idempotency contract changed.

`7377e46` adds the missing malformed-delivery case when
`AuthEmailRabbitListener` has no broker channel. The listener remains
non-throwing and performs no acknowledgement attempt; the focused listener
test and full repository JaCoCo run passed. The inventory remains 86 records
and falls from 158 to 157 missed branches. RabbitMQ acknowledgement/retry/DLQ
and deployed Mailpit evidence remain open. No production implementation or
contract changed.

`cc21733` adds signed-persona E2E coverage for Notifications `getPreferences`
and `updatePreferences`: default retrieval, persisted update, and isolation
from a second subject. It changes no production implementation or contract;
inbox listing/mark-read, broker delivery, and the remaining per-operation
authorization dimensions remain open.

`716e6aa` extends the same signed-persona product journey through Notifications
`markAsRead`: it asserts the 204 response and rereads the inbox to verify the
durable read flag. It changes no production implementation or contract; inbox
database-failure, duplicate-mark-read, broker, and remaining operation-matrix
dimensions remain open.

`5bb9981` adds the Expense Core outbox integration test for acknowledging an
unknown event ID. The test proves the durable outbox snapshot remains empty,
closing the residual no-op branch without changing production implementation,
contracts, or acknowledgement behavior for known events. The focused test and
full Java 25 Gradle wrapper run passed; the regenerated inventory is now 85
method records / 156 missed branches. Broker acknowledgement/retry/DLQ and
deployed messaging evidence remain open.

The current follow-up test increment adds an invalid-catalog-status case to
`GlobalErrorHandlerTest`. It exercises the handler's fail-safe internal-error
fallback without changing the error contract or deleting the defensive
mapping. The full Java 25 Gradle wrapper run passed; the regenerated inventory
remains 85 method records and falls from 156 to 154 missed branches. The
individual valid-status fallback arms remain structurally governed by the
immutable `ErrorCode` status invariant and are documented as such.

`dc68c73` extends `TokenSessionServiceTest` with the missing refresh-session
truth-table cases: revoked-only, replaced-only, and legacy accountless sessions.
Each case asserts the stable `ERR_03` fail-closed outcome, while the accountless
case also proves the persisted session is not mutated. The focused Accounts
suite and full Java 25 wrapper run passed; the regenerated inventory falls from
85 records / 154 missed branches to 84 records / 152 missed branches. Replay
concurrency and deployed passwordless-session evidence remain open.

`b0e51f7` adds MockMvc coverage for the Expense Core group-controller acceptance
faults: rollback on group update and fanout failure on member listing. The tests
assert the stable conflict and bad-gateway problem codes before the store is
called. The focused suite and full Java 25 wrapper run passed; the regenerated
inventory falls from 84 records / 152 missed branches to 82 records / 150 missed
branches. Deployed failure-path and signed-persona group E2E evidence remain
open.

`7bac0e2` adds RSA key-provider tests for blank PEM configuration values and an
explicitly blank rotation `kid`. These cases exercise fail-closed configuration
and key-identity boundaries without changing production logic or contracts. The
focused Accounts suite and full Java 25 wrapper run passed; the regenerated
inventory falls from 81 records / 148 missed branches to 79 records / 145
missed branches. Deployed key rotation and discovery evidence remain open.

`0be4bbc` adds a direct identity-equality assertion for
`IssuedCredential.equals`, covering the object-identity fast path without
changing the value-object contract or implementation. The focused Accounts
suite and full Java 25 wrapper run passed; the regenerated inventory falls
from 79 records / 145 missed branches to 78 records / 144 missed branches.
Passwordless replay and deployed delivery evidence remain open.

## Audit update through `aaa34fd`

The branch was re-audited from `0be4bbc` through `aaa34fd`. The intervening
coverage, acceptance-fixture, CI, and documentation commits do not delete any
file, change any contract, or modify production implementation under
`app/**/src/main` or `libs/**/src/main`. In particular, `c6e7c59` changes only
live acceptance fixtures, `1ed9557` changes CI fan-out, and the QA-10 commits
add tests or synchronize the ledger and evidence documents.

The latest coverage increments add nullable GraphQL `StringValue` literal tests
for `MoneyMinor` and `DateTime` and an invalid-existing-watermark test for the
BFF gateway; they do not remove or simplify implementation logic. The
authoritative inventory consequently moves from 59 methods / 113 missed
branches to 55 methods / 108 missed branches. The related commits are
`817a5b2`, `820a899`, the BFF absent-watermark behavior test, and the
already-bound-placeholder invitation test; the remaining residuals
are still open evidence work, not permission to delete implementation branches
or weaken public behavior.
