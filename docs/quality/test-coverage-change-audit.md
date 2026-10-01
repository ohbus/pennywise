# Test-coverage change audit

**Scope:** every branch commit from merge-base `65b2fb66d61d90b4d92e4e23a55e06f43cd89270`
(`master`) through the current branch tip `48fe0db`, whose subject indicates
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

As a reproducible history check, the subject-matching audit selected **80
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

The current branch tip was checked separately: the committed changes contain
test, tooling, CI, and documentation changes, with no production implementation
or contract-file change in the current coverage increment. No branch in this audit is
closed by deleting implementation logic, weakening a guard, changing a public
contract, or adding a coverage exclusion.

## Current evidence

After restoring implementation logic and retaining the added tests, the freshly
regenerated reports contain 147 methods with 346 missed branches. The remaining
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

The A07 audit increment names all 20 current method-level records with source
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
