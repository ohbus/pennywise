# GraphQL operation ownership

`me` maps to Accounts `GET /v1/me`. Group and expense queries/mutations map to
Expense Core. `recordRepayment` maps to Expense Core settlement APIs. The BFF
forwards stable idempotency keys and expected versions; it never calculates
balances. `groupChanged` carries only an authorized group ID, revision and
change ID. Clients refetch through the change feed after a subscription event.

GraphQL errors preserve REST problem codes in `extensions.code` and include a
request ID. Query depth, page size, aliases and subscription counts are bounded.

## Current implementation status

The BFF wires GraphQL operations to their respective service REST gateways:
- **Queries**:
  - `me`: Maps to Accounts `GET /accounts/v1/me` via `AccountsGateway`.
  - `groups`: Maps to Expense Core `GET /expense-core/v1/groups` with bounded member resolution (`flatMapSequential`).
  - `group(id)`: Maps to Expense Core `GET /expense-core/v1/groups/{groupId}` including members.
  - `settlementSuggestions(groupId)`: Maps to Expense Core `GET /expense-core/v1/groups/{groupId}/settlements/suggestions`.
- **Mutations**:
  - `createGroup`: Maps to Expense Core `POST /expense-core/v1/groups`.
  - `updateGroup`: Maps to Expense Core `PATCH /expense-core/v1/groups/{groupId}` and emits a live group invalidation.
  - `createExpense`: Maps to Expense Core `POST /expense-core/v1/groups/{groupId}/expenses` with idempotency forwarding and invalidation emission.
  - `recordRepayment`: Maps to Expense Core `POST /expense-core/v1/groups/{groupId}/settlements` and invalidation emission.
- **Subscriptions**:
  - `groupChanged(groupId)`: Subscribes to per-replica `LiveUpdateFanout` filtered by authorized `groupId`.

Error mapping preserves REST problem codes (e.g. `VALIDATION_FAILED`, `CONFLICT`, `UNAUTHORIZED`) in `extensions.code` along with `extensions.requestId`.
