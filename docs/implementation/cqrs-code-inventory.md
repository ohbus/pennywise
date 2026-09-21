# CQRS implementation code inventory

This inventory turns the CQRS plan into concrete repository work. It is based
on the current source tree and must be updated whenever a persistence path is
added or moved. Paths are repository-relative. The named classes and methods
are the first review targets; implementers must re-run the operation catalog
after each change.

## Shared database boundary

Current state: [`libs/db/build.gradle.kts`](../../libs/db/build.gradle.kts) only
exports `libs.boot.data.jpa`; it contains no source package, datasource
configuration, routing, transaction guard, policy, health, or tests.

Target files for DB-03 through DB-05:

- `libs/db/src/main/kotlin/com/subhrodip/pennywise/db/config/DbProperties.kt`
- `libs/db/src/main/kotlin/com/subhrodip/pennywise/db/config/DbAutoConfiguration.kt`
- `libs/db/src/main/kotlin/com/subhrodip/pennywise/db/routing/DbRoute.kt`
- `libs/db/src/main/kotlin/com/subhrodip/pennywise/db/routing/DbExecutionContext.kt`
- `libs/db/src/main/kotlin/com/subhrodip/pennywise/db/routing/DbRoutingDataSource.kt`
- `libs/db/src/main/kotlin/com/subhrodip/pennywise/db/routing/DbTransactionGuard.kt`
- `libs/db/src/main/kotlin/com/subhrodip/pennywise/db/policy/ReadPolicy.kt`
- `libs/db/src/main/kotlin/com/subhrodip/pennywise/db/health/ReaderHealthRegistry.kt`
- `libs/db/src/main/kotlin/com/subhrodip/pennywise/db/metrics/DbMetrics.kt`
- `libs/db/src/test/kotlin/com/subhrodip/pennywise/db/` (route, pool, guard, and failure tests)

The package spelling in the metrics path must match the chosen canonical
package; no duplicate technical package may be introduced.

Application integration targets are each application `build.gradle.kts`,
`src/main/resources/application.yml`, and application startup/configuration
classes. The BFF must not receive the library dependency.

## Expense Core command-side persistence

These paths remain writer-only and must be marked `COMMAND`, `LOCKING_QUERY`,
or `CLAIM` in the catalog:

- `groups/GroupRepository.kt`: `findLockedByGroupId` pessimistic group lock.
- `groups/GroupMembershipRepository.kt`: membership reads used during mutation,
  active-member checks, and membership updates.
- `groups/GroupInvitationRepository.kt`: invitation claim/revoke/update queries.
- `groups/JpaGroupStore.kt`: all `@Transactional` mutation methods, invitation
  claim, membership removal, group revision, audit, sync, and outbox effects.
- `expenses/JpaExpenseStore.kt`: create/update/delete, group lock, idempotency
  lookup/claim, membership revalidation, posting reversal, and materialization.
- `expenses/ExpenseRepository.kt`, `ExpenseIdempotencyRepository.kt`, and
  `BalancePostingRepository.kt`: aggregate, idempotency, and ledger queries.
- `settlements/JpaSettlementStore.kt`, `SettlementRepository.kt`: settlement
  recording, replay/conflict handling, reversal, and posting queries.
- `recurring/RecurringExpenseService.kt`, `RecurringExpenseWorker.kt`,
  `RecurringExpenseScheduleRepository.kt`, and
  `RecurringExpenseOccurrenceRepository.kt`: schedule mutations and due-row
  pessimistic claims.
- `sync/JpaSynchronizationStore.kt`, `SyncChangeRepository.kt`: revision
  allocation, tombstone writes, and cursor-sensitive reads.
- `messaging/JpaOutboxStore.kt`, `OutboxRepository.kt`, and
  `OutboxRelayDaemon.kt`: transactional append, `SKIP LOCKED` claim, ack,
  reject, retry, and reconciliation.

DB-06 must split these into command ports/handlers without moving business
authorization out of the feature slices. It must preserve the existing
transaction that combines financial postings, audit, sync, and outbox effects.

## Expense Core query-side candidates

The first query review targets are:

- `search/JpaSearchStore.kt`: search projection, filters, page size, stable
  creation ordering, and CSV result materialization. Candidate for DB-07.
- `expenses/JpaExpenseStore.kt`: read methods around lines 473–482 and any
  entity graph/materialization path. Separate list/detail DTO queries from
  mutation aggregate loading.
- `groups/JpaGroupStore.kt`: `@Transactional(readOnly = true)` group/member
  listing methods. They remain writer-bound when used for authorization; only
  a separately authorized historical view may become replica eligible.
- `sync/JpaSynchronizationStore.kt`: cursor/change reads remain writer-only
  until causal revision semantics are explicitly implemented in DB-10.
- `settlements/JpaSettlementStore.kt`: balance/suggestion reads remain writer
  bound initially because they expose current financial state.

Related controllers and contracts to inspect when response freshness changes:
`expenses/ExpenseController.kt`, `search/SearchController.kt`,
`groups/GroupController.kt`, `sync/SyncController.kt`,
`settlements/SettlementController.kt`, `contracts/rest/expense-core.json`,
`contracts/graphql/schema.graphqls`, and BFF gateway/resolver classes.

## Accounts command-side persistence

Writer-only paths for DB-08:

- `auth/credential/LoginCredentialService.kt` and
  `auth/credential/LoginCredentialRepository.kt`: credential creation,
  consume/revoke/update queries.
- `auth/session/TokenSessionService.kt` and `auth/session/AuthSessionRepository.kt`:
  refresh-token family rotation, revocation, and replay detection.
- `auth/abuse/LoginRateLimitService.kt` and `RateLimitBucketRepository.kt`:
  atomic bucket update queries.
- `auth/login/LoginVerificationService.kt`, `LoginStartService.kt`, and
  `auth/delivery/AuthEmailOutboxService.kt`: credential consumption and
  transactional delivery append.
- `auth/delivery/AuthEmailOutboxRepository.kt` and
  `AuthEmailOutboxPublisher.kt`: locked claim, acknowledgement, retry, and
  parking.
- `requests/deletion/JpaDeletionRequestStore.kt` and
  `requests/export/JpaExportRequestStore.kt`: request state transitions.

Replica candidates requiring privacy and causal review:

- `profile/JpaProfileStore.kt`, `ProfileRepository.kt`, and
  `ProfileController.kt` for profile lookup and bounded batch lookup.
- `profile/ProfileStore.kt` and `InMemoryProfileStore.kt` for port parity tests.

`auth/ProductionSecurityConfig.kt` and application resource configuration must
not acquire a reader route for authentication decisions.

## Notifications command-side persistence

Writer-only paths for DB-09:

- `inbox/JpaNotificationInboxStore.kt`, `NotificationInboxRepository.kt`, and
  `InboxController.kt`: mark-read mutation, subject-scoped reads used for
  authorization, and inbox state changes.
- `preferences/JpaPreferenceStore.kt`,
  `NotificationPreferenceRepository.kt`, and `PreferenceController.kt`:
  preference updates and preferences consulted before delivery.
- `consumer/TransactionalNotificationEventProcessor.kt`,
  `ProcessedNotificationEventRepository.kt`, and `JpaEventDeduplicator.kt`:
  event deduplication and transactional consumer state.
- `consumer/NotificationConsumerService.kt`,
  `delivery/InboxDeduplicator.kt`, and listener classes: retry/claim state
  that must not observe stale deduplication data.

Historical inbox browsing may become the first Notifications replica candidate,
but `inbox/JpaNotificationInboxStore.kt` must first expose a bounded DTO query
separate from mark-read and authorization paths.

## Configuration and infrastructure targets

Every stateful service currently has `src/main/resources/application.yml` with
JPA validation, Open Session in View disabled, and management metrics. DB-04
must update:

- `app/accounts/src/main/resources/application.yml`
- `app/expense-core/src/main/resources/application.yml`
- `app/notifications/src/main/resources/application.yml`
- corresponding Gradle dependencies/build files to consume `libs/db` and any
  JDBC/metrics support exactly once.

Flyway migration locations remain service-owned:

- `app/accounts/src/main/resources/db/migration/`
- `app/expense-core/src/main/resources/db/migration/`
- `app/notifications/src/main/resources/db/migration/`

No reader migration path may be added. Infrastructure work targets
`infra/local/`, `infra/deploy/`, `infra/observability/`,
`infra/versions.env.example`, and environment examples. Compose must render
standalone and merged configurations before any runtime claim is made.

## Test and evidence targets

Existing persistence and concurrency tests to preserve and extend include:

- Expense Core: `JpaExpenseStoreTest`, `JpaGroupStoreTest`,
  `JpaSettlementStoreTest`, `JpaSearchStoreTest`,
  `PostgresExpenseIdempotencyConcurrencyTest`,
  `PostgresMembershipMutationRaceTest`,
  `PostgresSettlementReconciliationTest`, and group concurrency tests.
- Accounts: `AuthPersistenceTest`, `JpaRequestStoresTest`, profile persistence,
  credential/session, rate-limit, and auth-email outbox tests.
- Notifications: `JpaPreferenceStoreTest`, `JpaNotificationInboxStoreTest`,
  consumer transaction/deduplication, and listener retry tests.
- Cross-cutting: `tests/performance/`, `tests/acceptance/`, `tools/contracts/`,
  `infra/observability/`, and `docs/operations/` release/restore procedures.

Every new test must assert the selected route or prove writer-only behavior;
green business tests without route evidence are insufficient.
