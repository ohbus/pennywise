# API implementation status

The REST OpenAPI documents describe the reviewed public surface. All 41 current
REST operations are wired to controllers, and stateful operation families use
the documented PostgreSQL/JPA adapters and transaction boundaries. Contract
path items that carry `x-implementation-status` are marked
`implemented-durable`; operation-level coverage maturity remains separate from
implementation status.

Current slices include Accounts profile/export requests, single and batch profile lookup (ACC-05 with duplicate deduplication), Expense Core groups/members/invites, allocation preview, expenses, balances, settlements, synchronization, and recurrence, plus Notifications preferences, inbox (including idempotent mark-as-read via NOT-09), delivery, and email consumption. The BFF exposes `me`, group queries and mutations (including group renaming via CORE-18/CORE-22 and bounded member fanout via BFF-06/BFF-07), expense/repayment operations, settlement suggestions, and live change subscriptions. Coverage and persistence maturity are tracked authoritatively in `docs/tasks/registry.yaml`.

Public-interface coverage is tracked separately from implementation status in
[`docs/quality/public-interface-coverage.md`](../quality/public-interface-coverage.md)
under QA-07. An implemented operation is not considered exhaustively covered
until its contract, transport, authorization, failure, concurrency, replay,
side-effect, and live integration evidence are recorded at the appropriate
level.

Current authorization and lifecycle invariants include:

- all group-scoped expense, settlement, recurring-schedule, and group-lifecycle
  writes/read operations require an authenticated active member;
- archived groups reject new expense writes with a conflict response, and
  archive replay is rejected;
- notification inbox and preference operations reject missing authentication;
- malformed cursors, invalid pagination bounds, malformed identifiers, and
  incompatible media negotiation have explicit problem/status coverage.

The operation-level evidence and remaining dimensions are maintained in
[`public-interface-operation-matrix.md`](../quality/public-interface-operation-matrix.md).
The latest local Docker evidence includes 86 live REST edge checks, but this
does not claim production OIDC, production-scale capacity, restore, multi-region
failover, security-scan, or deployment-rollback evidence.
