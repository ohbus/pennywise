# API implementation status

The REST OpenAPI documents describe the reviewed public surface. Operations that
are not wired to a controller are marked with `x-implementation-status:
planned`. Operations marked `implemented-in-memory` are callable in the current
scaffold, but their state is process-local until the documented PostgreSQL
adapters and transactional boundaries are completed.

Current slices include Accounts profile/export requests and profile lookup,
Expense Core groups/members/invites, allocation preview, expenses, balances,
settlements, synchronization, and recurrence, plus Notifications preferences,
inbox, delivery, and email consumption. The BFF exposes `me`, group queries and
mutations, expense/repayment operations, settlement suggestions, and change
subscriptions. Coverage and persistence maturity remain task-specific; CORE-18
and BFF-06 are still in progress, and the registry—not this summary—controls
completion status.
