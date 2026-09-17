# GraphQL operation ownership

`me` maps to Accounts `GET /v1/me`. Group and expense queries/mutations map to
Expense Core. `recordRepayment` maps to Expense Core settlement APIs. The BFF
forwards stable idempotency keys and expected versions; it never calculates
balances. `groupChanged` carries only an authorized group ID, revision and
change ID. Clients refetch through the change feed after a subscription event.

GraphQL errors preserve REST problem codes in `extensions.code` and include a
request ID. Query depth, page size, aliases and subscription counts are bounded.

## Current implementation status

The BFF currently wires `groups` and `createGroup` through the Expense Core REST
gateway. `me`, `group`, `createExpense`, and `recordRepayment` remain planned
contract operations; retaining them in the schema reserves the reviewed MVP
surface without claiming that their public transport handlers exist.
