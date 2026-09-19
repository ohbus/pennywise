# API implementation status

The REST OpenAPI documents describe the reviewed public surface. Operations that
are not wired to a controller are marked with `x-implementation-status:
planned`. Operations marked `implemented-in-memory` are callable in the current
scaffold, but their state is process-local until the documented PostgreSQL
adapters and transactional boundaries are completed.

Current slices include Accounts profile/export requests, single and batch profile lookup (ACC-05 with duplicate deduplication), Expense Core groups/members/invites, allocation preview, expenses, balances, settlements, synchronization, and recurrence, plus Notifications preferences, inbox (including idempotent mark-as-read via NOT-09), delivery, and email consumption. The BFF exposes `me`, group queries and mutations (including group renaming via CORE-18/CORE-22 and bounded member fanout via BFF-06/BFF-07), expense/repayment operations, settlement suggestions, and live change subscriptions. Coverage and persistence maturity are tracked authoritatively in `docs/tasks/registry.yaml`.
