# Task board

The coordinator owns this board and `registry.yaml`. The registry is the source
of truth. Its JSON formatting is valid YAML 1.2 and permits dependency-free
validation with Python's standard library.

## CQRS data-access and PostgreSQL reader-scaling milestone

The implementation-ready code inventory is maintained in
`docs/implementation/cqrs-code-inventory.md`. These tasks are intentionally
writer-safe: no reader routing is enabled until the shared kernel, route guards,
query classification, and evidence gates are complete.

| ID | Owner | Status | Deliverable |
| --- | --- | --- | --- |
| DB-01 | coordinator | done | Catalog every persistence operation and register the workstream |
| DB-02 | architecture | done | Define command/query, consistency, watermark, fallback, and retry contracts |
| DB-03 | coordinator | done | Implement `libs/db` route context, policies, and transaction guards |
| DB-04 | platform | done | Add separate writer/named-reader pools and writer-only migration wiring |
| DB-05 | platform | done | Add reader lag health, circuit breaking, and bounded fallback |
| DB-06 | core | done | Split Expense Core command/query ports while preserving financial transactions |
| DB-07 | core | done | Pilot bounded Expense Core search projection and measured query optimization |
| DB-08 | accounts | done | Split Accounts command/query ports with writer-only auth state |
| DB-09 | notifications | done | Split Notifications command/query ports with writer-only delivery state |
| DB-10 | coordinator | in_progress | Propagate causal writer watermarks through services and BFF |
| DB-11 | observability | in_progress | Add query operation telemetry and slow-query governance |
| DB-12 | quality | in_progress | Add contention, replica failure, lag, and capacity evidence |
| DB-13 | platform | done | Add optional local/production-like PostgreSQL replica topology |
| DB-14 | coordinator | in_progress | Run one reviewed historical-read replica pilot |
| DB-15 | coordinator | done | Promote only individually approved query capabilities |
| DB-16 | operations | in_progress | Complete failover, restore, rollback, alert, and release gates |
| DB-17 | coordinator | in_progress | Reconcile implementation and evidence against every plan requirement |

## Current milestone: documentation and contracts

## Exhaustive public-interface coverage

| ID | Owner | Status | Deliverable |
| --- | --- | --- | --- |
| QA-07 | coordinator | in_progress | Contract-driven REST, GraphQL, WebSocket, negative-path, concurrency, recovery, and evidence matrix |
| QA-08 | coordinator | planned | Production-scale, deployment-resilience, security, and unresolved WebSocket protocol evidence |

## Production hardening milestone

| ID | Owner | Status | Deliverable |
| --- | --- | --- | --- |
| PR-41 | coordinator | in_progress | Make BFF financial fanout fail closed |
| PR-42 | coordinator | in_progress | Bound RabbitMQ transient redelivery |
| PR-43 | coordinator | in_progress | Wire configurable GraphQL subscription bounds |
| PR-44 | coordinator | in_progress | Serialize GraphQL subscription admission |
| PR-45 | coordinator | in_progress | Lock recurring schedules during worker claims |
| PR-46 | coordinator | in_progress | Add durable settlement recording idempotency |
| PR-47 | coordinator | in_progress | Add PostgreSQL populated-settlement reconciliation evidence |
| PR-48 | coordinator | in_progress | Add CI dependency vulnerability review gate |
| PR-49 | coordinator | in_progress | Bound GraphQL transport request sizes |
| AUTH-01 | coordinator | done | Provider-neutral OIDC authentication hardening baseline and implementation tracker |
| AUTH-02 | coordinator | done | Remove implicit authentication identities with full boundary evidence |
| AUTH-03 | coordinator | done | Fail-closed provider-neutral OIDC resource-server validation |
| AUTH-04 | coordinator | done | Validate OIDC JWT claims, signatures, expiry, and subjects |
| AUTH-05 | coordinator | done | Remove weaker local authentication modes and require local OIDC parity |
| AUTH-06 | coordinator | done | Keycloak environment, real OIDC journeys, and meaningful Compose hostnames |
| PR-17 | coordinator | in_progress | Financial ledger reconciliation and durable mutation idempotency |
| PR-18 | coordinator | in_progress | Bounded persistence reads and mutation-time authorization |
| PR-19 | coordinator | in_progress | Messaging retry, dead-letter, and poison-message handling |
| PR-20 | coordinator | in_progress | GraphQL abuse controls |
| PR-21 | coordinator | in_progress | CI, security, SBOM, and architecture gates |
| PR-22 | coordinator | in_progress | Remove production in-memory persistence fallbacks |
| PR-23 | coordinator | in_progress | Profile and financial adapter production wiring |
| PR-24 | coordinator | in_progress | Remove Accounts request-service in-memory defaults |
| PR-25 | coordinator | in_progress | Fail closed on production identity-provider wiring |
| PR-26 | coordinator | in_progress | Expense participant and request-bound validation |
| PR-27 | coordinator | in_progress | Notification fail-closed delivery policy |
| PR-28 | coordinator | in_progress | BFF fail-closed upstream configuration |
| PR-29 | coordinator | in_progress | Checked financial arithmetic |
| PR-30 | coordinator | in_progress | Subscription revocation on membership removal |
| PR-31 | coordinator | in_progress | Required production messaging capabilities |
| PR-32 | coordinator | in_progress | Deployment overlay configuration alignment |
| PR-33 | coordinator | in_progress | Security hygiene fixture classification |
| PR-34 | coordinator | in_progress | Notification log redaction |
| PR-35 | coordinator | in_progress | Actuator exposure hardening |
| PR-36 | coordinator | in_progress | Versioned notification queue topology |
| PR-37 | coordinator | in_progress | GraphQL abuse-control transport evidence |
| PR-38 | coordinator | in_progress | CI workflow and release-gate parity |
| PR-39 | coordinator | in_progress | Production application topology controls |
| PR-40 | coordinator | in_progress | Idempotency retention and cleanup |
| AUTH-07 | coordinator | done | Pennywise-owned passwordless login, token lifecycle, provider portability, and authorization evidence |
| OPS-24 | coordinator | done | Remove undeclared Ruby dependency and E2E Compose host-port collisions from CI |
| OPS-17 | coordinator | done | Stable error taxonomy and service/source attribution |
| OPS-18 | coordinator | done | Micrometer and Prometheus metrics for all services |
| OPS-19 | operations | done | Dashboards, alerts, SLOs, and runbooks baseline |
| OPS-20 | platform | done | Reliability, security, and capacity release gates |
| OPS-21 | platform | done | Modular k6 load tests for high-value endpoints |
| OPS-22 | platform | done | 1M-user capacity baseline and production readiness evidence |
| OPS-23 | platform | done | Fixture-backed mutation capacity scenarios |

## Error reporting hardening workstream

| ID | Owner | Status | Deliverable |
| --- | --- | --- | --- |
| ERR-01 | coordinator | done | Governed error taxonomy, catalog, naming, and validation |
| ERR-02 | coordinator | done | Typed shared error definitions, exceptions, problem envelope, and error IDs |
| ERR-03 | coordinator | done | Structured 401/403 security errors across REST services and BFF |
| ERR-04 | accounts | done | Accounts-specific error migration |
| ERR-05 | core | done | Expense Core groups and membership error migration |
| ERR-06 | core | done | Expense Core expense and idempotency error migration |
| ERR-07 | core | done | Settlement, recurrence, sync, and outbox error migration |
| ERR-08 | notifications | done | Notifications and event-consumer error migration |
| ERR-09 | bff | done | Upstream and GraphQL error mapping |
| ERR-10 | quality | done | Contract, unit, integration, Bruno, and acceptance coverage |
| ERR-11 | operations | done | Bounded error metrics, dashboards, and alerts |
| ERR-12 | coordinator | done | Governance review, release evidence, and completion gate |

| ID | Owner | Status | Deliverable |
| --- | --- | --- | --- |
| DOC-01 | coordinator | done | Tracker, working agreement, task details |
| DOC-02 | architecture agent | done | Product scope and ownership |
| DOC-03 | architecture agent | done | Technology, security, persistence, structure |
| DOC-04 | coordinator | done | Financial rules and REST contracts |
| DOC-05 | coordinator | done | Events, recurrence, offline contracts |
| DOC-06 | coordinator | done | GraphQL schema and operation mapping |
| DOC-07 | quality agent | done | Acceptance, operations, recovery and capacity |
| DOC-08 | coordinator | done | Cross-review and documentation gate |
| DOC-09 | coordinator | done | Mermaid visual documentation with Docker Compose rendering |
| DOC-10 | coordinator | done | Version-controlled IntelliJ run configurations |
| DOC-11 | coordinator | done | Endpoint-specific pagination policy and bounded collection guidance |
| DOC-12 | coordinator | done | Establish compatible Kotlin quality and coding guidelines |
| DOC-13 | coordinator | done | Repository-wide implementation review and empty-folder cleanup |
| DOC-14 | lint_research | done | Evaluate Kotlin lint compatibility |
| DOC-15 | coordinator | done | Repository-wide plan and implementation drift audit |
| DOC-15A | drift_architecture | done | Architecture, contract, and documentation drift review |
| DOC-15B | drift_build | done | Code, build, CI, and test drift review |
| DOC-16 | contract_reconciliation | done | Reconcile API contracts with implemented endpoints |
| FND-01 | coordinator | done | Verified Gradle scaffold and UI placeholder |
| FND-02 | platform | done | Local infrastructure and migration foundations |
| FND-03 | quality | done | CI and executable quality checks |
| FND-04 | coordinator | done | Repository workflow Makefile |
| FND-05 | coordinator | done | Shared REST error flow |
| FND-06 | coordinator | done | Central dependency and plugin catalog |
| FND-07 | uuidv7 | done | Centralized UUIDv7 generation abstraction |
| QA-01 | acceptance_harness | done | Integrated product acceptance harness |
| QA-02 | coordinator | done | Scaffold tests, smoke E2E, and coverage baseline |
| OPS-01 | deployment_release | done | Deployment telemetry and release hardening |
| OPS-02 | recovery_capacity | done | Recovery, capacity, cost, and launch verification |
| OPS-03 | coordinator | done | Modular Dockerfiles and Compose operations |
| OPS-04 | coordinator | done | Configurable PR, branch, and main CI workflows with GHCR image delivery |
| OPS-05 | coordinator | done | CI hardening review and local parity improvements |
| OPS-06 | coordinator | done | Centralized aggressive reusable CI caching |
| OPS-07 | coordinator | done | Dependency service containers and local health checks |
| ACC-01 | accounts_slice | done | Accounts profile contract slice with validation and authenticated access |
| ACC-02 | accounts_persistence | done | Durable account deletion and export request persistence |
| CORE-01 | groups_membership | done | Groups, membership, and invitations |
| CORE-02 | coordinator | done | Allocation preview and financial domain foundation |
| CORE-03 | coordinator | done | Settlement record and idempotent reversal domain slice |
| CORE-04 | sync_slice | done | Offline synchronization and cursor snapshots |
| CORE-05 | recurrence_slice | done | Recurring expense generation |
| CORE-06 | search_export | done | Search and export boundaries |
| CORE-07 | expense_categories | done | Categorized expenses and category management |
| CORE-08 | expense_persistence | done | Durable Expense Core settlement persistence adapter |
| CORE-09 | group_persistence | done | Durable group, membership, and invitation persistence |
| CORE-10 | outbox_persistence | done | Durable transactional outbox persistence |
| CORE-11 | sync_persistence | done | Durable synchronization change persistence |
| CORE-12 | outbox_relay_daemon | done | Outbox background polling relay daemon |
| CORE-13 | expense_persistence | done | Durable expense ledger entity and posting persistence |
| CORE-14 | expense_persistence | done | Durable expense update, deletion, and posting reversal |
| MSG-01 | outbox_delivery | done | Outbox and broker delivery |
| NOT-01 | notifications_slice | done | Notification inbox and delivery |
| NOT-02 | notification_persistence | done | Durable Notifications preference persistence adapter |
| NOT-03 | inbox_persistence | done | Durable notification inbox persistence |
| NOT-04 | notification_consumer | done | Transactional notification event consumption |
| NOT-05 | rabbit_listener | done | RabbitMQ listener and acknowledgement adapter |
| BFF-01 | bff_gateway | done | GraphQL BFF gateway adapters |
| BFF-02 | live_updates_slice | done | BFF live update fanout |
| BFF-03 | bff_gateway | done | GraphQL BFF query and mutation resolvers |
| CORE-15 | recurring_agent | done | Durable recurring expense schedules, occurrences, and runner |
| QA-03 | acceptance_agent | done | Real multi-service acceptance test scenarios |
| NOT-06 | email_agent | done | SMTP email dispatch adapter in Notifications service |
| ACC-03 | accounts_agent | done | Expose GDPR export request REST endpoints in Accounts service |
| CORE-16 | settlement_agent | done | Debt simplification and settlement suggestions engine |
| BFF-04 | bff_agent | done | Settlement suggestions GraphQL resolver |
| NOT-07 | consumer_agent | done | Connect notification consumer with email dispatch |
| OPS-08 | devops_agent | done | Compose dev environment configuration hardening and live acceptance workflow |
| CORE-17 | group_agent | done | Expose group members endpoint and durable member listing in Expense Core |
| BFF-05 | subscription_agent | done | Implement groupChanged GraphQL subscription with reactive live update sink |
| NOT-08 | pref_agent | done | Complete Notifications preferences contract schema and persistence validation |
| ACC-04 | account_agent | done | Expose public profile lookup endpoint by account ID in Accounts service |
| CORE-18 | group_update_agent | done | Group rename slice and hardening complete |
| BFF-06 | group_graphql_agent | done | Group update/member slice completed with bounded member resolution |
| NOT-09 | read_agent | done | Add mark inbox notification as read endpoint in Notifications service |
| ACC-05 | batch_account_agent | done | Expose batch profile lookup REST endpoint in Accounts service |
| DOC-17 | coordinator | done | Reconcile task state, Git ownership, and repository drift |
| DOC-17A | task_state_audit | done | Audit tracker state and evidence consistency |
| DOC-17B | git_task_map | done | Map commits and worktree files to tasks |
| DOC-17C | drift_review | done | Review implementation, contracts, tests, and scope drift |
| OPS-09 | compose_topology | done | Add dependency-only, standalone-app, and full-stack Compose workflows |
| CORE-19 | core | done | Complete group lifecycle and membership administration |
| CORE-20 | core | done | Expose recurring schedule management and pause notifications |
| CORE-21 | core | done | Complete authorized persistent search and CSV transport |
| CORE-22 | coordinator | done | Harden group updates with transactional change effects |
| BFF-07 | coordinator | done | Complete bounded GraphQL group-member resolution |
| MSG-02 | messaging | done | Deliver committed group changes to every BFF replica |
| QA-04 | coordinator | done | Execute authenticated real-dependency product acceptance |
| OPS-10 | platform | done | Produce public-launch restore, capacity, and cost evidence |
| DOC-18 | contracts | done | Reconcile current API operation and error contracts |
| DOC-19 | coordinator | done | Backfill legacy tracker ownership and evidence |
| DOC-20 | coordinator | done | Establish a non-breaking Kotlin formatting baseline |
| OPS-11 | coordinator | done | Fix Buildx GHA cache export for main image publishing |
| DOC-22 | coordinator | done | Apply programming principles and reconcile current documentation |
| DOC-23 | coordinator | done | Mandate continuous documentation updates and Javadoc/KDoc comments in working agreement |
| OPS-12 | coordinator | done | Isolate GHCR image publishing from reusable verification workflow |
| OPS-13 | coordinator | done | Align CI triggers, workflows, and documentation with master branch |
| OPS-14 | coordinator | done | Enable E2E smoke checks on feature branch CI |
| CORE-23 | core23_agent | done | Verify group-rename atomic rollback and effect payloads |
| CORE-24 | core | done | Cover group-rename request and authorization edge cases |
| CORE-25 | core25_agent | done | Verify group-rename concurrency against PostgreSQL |
| BFF-08 | bff | done | Test bounded group-member fanout behavior |
| IDE-01 | coordinator | done | Resolve IntelliJ GraphQL schema detection and eliminate unnecessary constructor field injection |
| BFF-09 | bff09_agent | done | Verify GraphQL transport error mapping for group operations |
| QA-05 | qa05_agent | done | Add public-interface edge-case acceptance journeys |
| BFF-10 | bff10_audit | done | Resolve GraphQL scalar deprecation warnings |
| BFF-11 | bff11_agent | done | Expose and verify the GraphQL HTTP transport route |
| QA-06 | coordinator | done | Isolate JpaOutboxStoreTest from inter-suite database pollution |
| OPS-15 | coordinator | done | Publish CI test results, pass down built application artifacts, and optimize caching |
| OPS-16 | coordinator | done | Standardize top-level CI environment and Node 24 runtime enforcement |
| DOC-24 | coordinator | done | Mandate strict SOLID file separation, comprehensive documentation linking, and pre-implementation documentation review |
| CORE-26 | core | done | Refactor Expense Core groups and settlements persistence into separated SOLID files |
| ACC-06 | accounts | done | Refactor Accounts persistence into separated SOLID files |
| NOT-10 | notifications | done | Refactor Notifications persistence into separated SOLID files |
| CORE-27 | core | done | Refactor Expense Core expenses, sync, and outbox persistence into separated SOLID files |
| CORE-28 | core | done | Refactor Expense Core recurring persistence into separated SOLID files |
| FND-08 | coordinator | done | Refactor domain ports, in-memory stores, and consumer services into dedicated files |
| OBS-01 | coordinator | done | Implement cross-cutting structured logging, MDC correlation, and observability tools |
| DOC-26 | coordinator | done | Reconcile local smoke-demo delivery evidence and Bruno API collection |
