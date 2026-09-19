# Public operation evidence matrix

This is the operation-level companion to
[`public-interface-coverage.md`](public-interface-coverage.md). The operation
inventory is contract-generated; a row is not complete merely because its
request is present in Bruno. “Remaining dimension work” is an explicit gap list
and must be reduced with endpoint-specific tests before QA-07 can close.

| Service | Method | Path | Operation | Current evidence | Remaining dimension work |
|---|---|---|---|---|---|
| Accounts | GET | `/me` | `getMe` | contract + GraphQL/E2E live | auth/validation/failure matrix |
| Accounts | PATCH | `/me` | `updateMe` | contract + live smoke + invalid/empty-patch edges | failure matrix |
| Accounts | POST | `/me/deletion-request` | `requestDeletion` | contract + live smoke + unauthenticated edge | service failure/replay matrix |
| Accounts | POST | `/me/export-request` | `requestExport` | contract + live smoke + unauthenticated edge | service failure/replay matrix |
| Accounts | GET | `/me/export-requests` | `listExportRequests` | contract + live smoke + unauthenticated edge | pagination/service failure matrix |
| Accounts | GET | `/profiles/{accountId}` | `getProfileById` | contract + live smoke + malformed-ID edge | failure matrix |
| Accounts | POST | `/profiles/batch` | `getProfilesBatch` | contract + controller + live smoke + empty/malformed/over-limit input + duplicate-ID deduplication | production dependency-failure evidence (QA-08) |
| Expense Core | POST | `/groups` | `createGroup` | contract + GraphQL/E2E live + authentication/invalid-kind edges | failure matrix |
| Expense Core | GET | `/groups` | `listGroups` | contract + live smoke + E2E + authentication/archived-state edges | validation/failure matrix |
| Expense Core | GET | `/groups/{groupId}` | `getGroup` | contract + live smoke + E2E + authentication/non-member/archived edges | validation/failure matrix |
| Expense Core | PATCH | `/groups/{groupId}` | `updateGroup` | contract + live smoke + E2E + non-member/blank-input edges | conflict/failure matrix |
| Expense Core | POST | `/allocations/preview` | `previewAllocation` | contract + live smoke + authentication/validation edges | failure matrix |
| Expense Core | POST | `/groups/{groupId}/archive` | `archiveGroup` | contract + live smoke + authentication/non-member/replay edges | archived-state side-effect matrix |
| Expense Core | GET | `/groups/{groupId}/members` | `listGroupMembers` | contract + live smoke + non-member/archived authorization | validation/failure matrix |
| Expense Core | POST | `/groups/{groupId}/placeholders` | `createPlaceholder` | contract + live smoke + non-member/blank-input edges | resource-state matrix |
| Expense Core | DELETE | `/groups/{groupId}/members/{membershipId}` | `removeGroupMember` | contract + controller + live smoke + non-member/malformed-ID authorization + removal replay conflict | live replay confirmation |
| Expense Core | POST | `/groups/{groupId}/invites` | `createInvite` | contract + live smoke + non-member authorization + expiry-boundary edges | replay matrix |
| Expense Core | POST | `/groups/{groupId}/invites/{token}/revoke` | `revokeInvite` | contract + controller + live success + non-member authorization + unknown/revoked-claim edges + revocation replay conflict | live replay confirmation |
| Expense Core | POST | `/invites/{token}/claim` | `claimInvite` | contract + live auth/success + invalid-token/replay/revoked edges | expiry matrix |
| Expense Core | POST | `/groups/{groupId}/expenses` | `createExpense` | contract + persistence + live + non-member authorization + archived-state edge | retry/dependency/timeout matrix |
| Expense Core | GET | `/groups/{groupId}/expenses` | `listExpenses` | contract + live smoke + non-member authorization + limit bounds (1..100) | production dependency failure matrix |
| Expense Core | PUT | `/groups/{groupId}/expenses/{expenseId}` | `updateExpense` | contract + persistence + live + non-member authorization | retry/dependency/timeout matrix |
| Expense Core | DELETE | `/groups/{groupId}/expenses/{expenseId}` | `deleteExpense` | contract + live smoke + non-member authorization | validation/failure matrix |
| Expense Core | POST | `/groups/{groupId}/settlements` | `recordSettlement` | contract + controller/persistence + live + non-member authorization + non-numeric/zero/same-participant validation | production dependency-failure evidence (QA-08) |
| Expense Core | GET | `/groups/{groupId}/settlements/suggestions` | `getSettlementSuggestions` | contract + controller/GraphQL/E2E live + empty-result + non-member authorization | production timeout/failure evidence (QA-08) |
| Expense Core | POST | `/groups/{groupId}/settlements/{settlementId}/reversal` | `reverseSettlement` | contract + controller/persistence + live authorization + not-found + idempotent replay preserving original reason | production dependency-failure evidence (QA-08) |
| Expense Core | GET | `/groups/{groupId}/balances` | `getBalances` | contract + persistence + live + non-member authorization | validation/failure matrix |
| Expense Core | GET | `/groups/{groupId}/sync/snapshot` | `getSnapshot` | contract + persistence + live + malformed/expired-cursor/non-member edges | broader cursor ownership cases |
| Expense Core | GET | `/groups/{groupId}/sync/changes` | `getChanges` | contract + persistence + live + malformed/expired-cursor/non-member edges | broader cursor ownership cases |
| Expense Core | GET | `/groups/{groupId}/search` | `searchExpenses` | contract + persistence + live + query/currency/category filters + cursor pagination/totals + malformed-cursor/non-member edges | production dependency-failure evidence (QA-08) |
| Expense Core | GET | `/groups/{groupId}/export` | `exportExpenses` | contract + persistence + live + query/currency/category filters + row bounds/formula-injection protection + non-member/media-negotiation edges | production dependency-failure evidence (QA-08) |
| Expense Core | POST | `/groups/{groupId}/schedules` | `createRecurringSchedule` | contract + live smoke + non-member authorization | recurrence validation/failure matrix |
| Expense Core | GET | `/groups/{groupId}/schedules` | `listRecurringSchedules` | contract + live smoke + non-member authorization | persistence/failure matrix |
| Expense Core | GET | `/groups/{groupId}/schedules/{scheduleId}` | `getRecurringSchedule` | contract + live smoke + non-member authorization | not-found/failure matrix |
| Expense Core | PUT | `/groups/{groupId}/schedules/{scheduleId}` | `updateRecurringSchedule` | contract + live smoke + explicit principal enforcement | validation/state-transition matrix |
| Expense Core | POST | `/groups/{groupId}/schedules/{scheduleId}/pause` | `pauseRecurringSchedule` | contract + persistence/controller lifecycle + live smoke + non-member authorization + idempotent paused replay | production dependency-failure evidence (QA-08) |
| Expense Core | POST | `/groups/{groupId}/schedules/{scheduleId}/resume` | `resumeRecurringSchedule` | contract + persistence/controller lifecycle + live smoke + explicit principal enforcement + idempotent resumed replay | production dependency-failure evidence (QA-08) |
| Notifications | GET | `/inbox` | `listInbox` | contract + persistence + live + auth/limit-boundary/cursor edges | failure/retry matrix |
| Notifications | POST | `/inbox/{notificationId}/read` | `markAsRead` | contract + persistence + live + authentication/malformed-ID/unknown-resource edges | failure/retry matrix |
| Notifications | GET | `/preferences` | `getPreferences` | contract + live smoke + unauthenticated edge | persistence/failure matrix |
| Notifications | PUT | `/preferences` | `updatePreferences` | contract + live smoke + unauthenticated edge | persistence/failure matrix |

## GraphQL and WebSocket operation matrix

| Surface | Operation | Current evidence | Remaining dimension work |
|---|---|---|---|
| GraphQL query | `me` | schema + resolver/controller + HTTP success + live authenticated/unauthenticated journey + redacted upstream-timeout HTTP envelope | production retry/timeout policy evidence (QA-08) |
| GraphQL query | `groups` | schema + resolver/controller + HTTP success + live empty-result journey + redacted timeout transport + live Expense Core outage envelope and recovery | production retry/timeout policy evidence (QA-08) |
| GraphQL query | `group` | schema + resolver/controller + HTTP success + live malformed-ID/not-found/non-member error envelopes + auth/validation/timeout/malformed-upstream transport tests | production retry policy evidence (QA-08) |
| GraphQL query | `settlementSuggestions` | schema + resolver/controller + HTTP success/empty/upstream-failure envelopes + live malformed-ID/non-member errors + live journey | production retry/timeout policy evidence (QA-08) |
| GraphQL mutation | `createGroup` | schema + resolver/controller + HTTP success + redacted conflict envelope + live malformed-enum validation/journey | production retry policy evidence (QA-08) |
| GraphQL mutation | `updateGroup` | schema + resolver/controller + HTTP success + validation/timeout error transport + live non-member authorization | production retry policy evidence (QA-08) |
| GraphQL mutation | `createExpense` | schema + resolver/controller + HTTP success + malformed-input/dependency-failure transport + live idempotent/tampered-replay edges + REST side-effect E2E | production retry policy evidence (QA-08) |
| GraphQL mutation | `recordRepayment` | schema + resolver/controller + HTTP success + conflict transport + live malformed-money/non-member authorization/journey | production retry policy evidence (QA-08) |
| GraphQL subscription | `groupChanged` | schema + resolver authorization + fanout unit tests + malformed-operation, authenticated/non-member, disconnect/reconnect, resubscription, and completed-subscription filtering WebSocket E2E | production replay/backpressure protocol evidence (QA-08) |
| WebSocket transport | `graphql-transport-ws` connection lifecycle | protocol handshake, subscribe, malformed-operation error frame, event delivery, authenticated resubscription after disconnect, and concurrent invalidation E2E | malformed frames, duplicate subscribe, timeout, and sustained backpressure evidence (QA-08) |

The local QA-07 package now covers success, validation, authorization, upstream
error/timeout conversion, redaction, empty results, replay, and observable side
effects across every GraphQL HTTP operation. The remaining GraphQL/WebSocket
items require production-like retry, timing, reconnect replay, and sustained
pressure evidence and are owned explicitly by QA-08; schema parity and mocked
transport tests are not presented as that evidence.
