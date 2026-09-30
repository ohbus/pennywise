# Public operation evidence matrix

This is the operation-level companion to
[`public-interface-coverage.md`](public-interface-coverage.md). The operation
inventory is contract-generated; the current validator reports 45 REST
operations and 9 GraphQL root operations. QA-10's repeatable source-reference
discovery is maintained in
[`test-coverage-gap-audit.md`](test-coverage-gap-audit.md) and
`tools/coverage/report_operation_test_gaps.py`. A row is not complete merely because its
request is present in Bruno. "Remaining dimension work" is an explicit gap list
and is now owned by QA-10 after QA-07's inventory milestone; it must be reduced
with endpoint-specific tests before QA-10 can close.

> **SEC-01 remediation update (2026-09-29):** The following SEC-01 workstreams
> have been implemented and verified with focused unit and integration tests:
> - **SEC-01A (SEC-001, SEC-009):** Squarewise-owned RS256 asymmetric token authority, RFC 8414
>   JWKS/metadata discovery, and in-memory JWKSource validation deployed in production profile.
> - **SEC-01B (SEC-002):** Durable `(issuer, providerSubject)` identity mapping; email is
>   verified contact data; profile no longer provisioned implicitly on bearer reads.
> - **SEC-01C (SEC-004, SEC-005):** Profile `GET /profiles/{accountId}` and `POST /profiles/batch`
>   now require self-lookup or `X-Squarewise-Workload-Role: internal-service` header; 403 on
>   unauthorized cross-account reads.
> - **SEC-01D (SEC-003, SEC-006):** WebSocket subscriptions auto-terminate on membership removal
>   via `LiveUpdateFanout` revocation signals; CSRF double-submit extended to cookie-authenticated
>   `POST /graphql` mutations.
> - **SEC-01E (SEC-007, SEC-008):** Trusted-proxy-aware `ClientAddressResolver` with IPv4/IPv6
>   normalization; access-token residual revocation window documented and bounded at 10 minutes.
> - **SEC-01F (SEC-010, SEC-011, SEC-012, SEC-013):** Structured `SecurityAuditLogger` emitting
>   redacted events for all key auth lifecycle outcomes; SEC-010 key rotation runbook with
>   verified overlap tests; SEC-011 supply chain evidence recorded.
>
> P0/P1 findings from the whole-security audit are now verified closed at the code and local
> test level. Production-scale, hosted-environment, and managed-provider evidence remains
> formally release-gated under QA-08 and OPS-20.
>
> The verified token-authority path is the Squarewise-owned RS256 issuer. The
> optional `squarewise.security.oidc.external-provider.enabled=true` path is
> not included in that closure: it remains a documented QA10-A04 gap until the
> external exchange is implemented or the property is rejected at startup.

| Service | Method | Path | Operation | Current evidence | Remaining dimension work |
|---|---|---|---|---|---|
| Accounts | POST | `/auth/login/start` | `startLogin` | contract + controller + unit tests + rate limit integration + **SEC-01E: trusted-proxy network partition** + **SEC-01F: LOGIN_RATE_LIMITED audit event** | failure/network partition matrix |
| Accounts | POST | `/auth/login/verify` | `verifyLogin` | contract + controller + unit tests + single-use redemption + **SEC-01A: asymmetric RS256 token issuance** + **SEC-01B: issuer/subject identity** + **SEC-01F: LOGIN_SUCCESS/LOGIN_FAILURE/IDENTITY_ENROLLED audit events** | failure/replay matrix |
| Accounts | POST | `/auth/token/refresh` | `refreshToken` | contract + controller + unit tests + rotation + reuse detection + **SEC-01F: TOKEN_REFRESHED/TOKEN_REUSE_DETECTED/SESSION_DENIED_DELETION_REQUESTED/SESSION_SUBJECT_MISMATCH audit events** | revocation matrix |
| Accounts | POST | `/auth/logout` | `logout` | contract + controller + unit tests + authentication required + **SEC-01F: SESSION_REVOKED audit event** | session revocation matrix |
| Accounts | GET | `/me` | `getMe` | contract + controller + GraphQL/E2E live + authenticated profile shape | unauthenticated/failure matrix |
| Accounts | PATCH | `/me` | `updateMe` | contract + controller/live smoke + validated fields + empty-patch edge | failure matrix |
| Accounts | POST | `/me/deletion-request` | `requestDeletion` | contract + live smoke + unauthenticated edge | service failure/replay matrix |
| Accounts | POST | `/me/export-request` | `requestExport` | contract + live smoke + unauthenticated edge | service failure/replay matrix |
| Accounts | GET | `/me/export-requests` | `listExportRequests` | contract + controller/live smoke + unauthenticated and invalid-subject edges | pagination/service failure matrix |
| Accounts | GET | `/profiles/{accountId}` | `getProfileById` | contract + controller/live smoke + malformed-ID, not-found, self-lookup, cross-account 403, and workload role edges + **SEC-01C: object authorization enforced** | failure matrix |
| Accounts | POST | `/profiles/batch` | `getProfilesBatch` | contract + controller + live smoke + empty/malformed/over-limit input + duplicate-ID deduplication + self-lookup and workload role authorization (403 for unauthorized cross-account batch) + **SEC-01C: batch object authorization enforced** | production dependency-failure evidence (QA-08) |
| Expense Core | POST | `/groups` | `createGroup` | contract + controller/GraphQL/E2E live + authentication/invalid-kind edges | production dependency-failure evidence (QA-08) |
| Expense Core | GET | `/groups` | `listGroups` | contract + controller/live smoke + E2E + authentication/archived-state edges | production dependency-failure evidence (QA-08) |
| Expense Core | GET | `/groups/{groupId}` | `getGroup` | contract + controller/live smoke + E2E + authentication/non-member/not-found/archived edges | production dependency-failure evidence (QA-08) |
| Expense Core | PATCH | `/groups/{groupId}` | `updateGroup` | contract + controller/live smoke + E2E + non-member/blank-input edges + revision increment | production dependency-failure evidence (QA-08) |
| Expense Core | POST | `/allocations/preview` | `previewAllocation` | contract + controller/live smoke + authentication and participant/percentage validation + exact-total invariants | production dependency-failure evidence (QA-08) |
| Expense Core | POST | `/groups/{groupId}/archive` | `archiveGroup` | contract + controller/live smoke + authentication/non-member/replay edges + archived mutation blocking | production dependency-failure evidence (QA-08) |
| Expense Core | GET | `/groups/{groupId}/members` | `listGroupMembers` | contract + controller/live smoke + non-member/archived authorization + claimed-placeholder lifecycle | production dependency-failure evidence (QA-08) |
| Expense Core | POST | `/groups/{groupId}/placeholders` | `createPlaceholder` | contract + controller/live smoke + non-member/blank-input edges + targeted invite binding | production dependency-failure evidence (QA-08) |
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
| Expense Core | POST | `/groups/{groupId}/schedules` | `createRecurringSchedule` | contract + controller/live smoke + non-member authorization + lifecycle validation | production dependency-failure evidence (QA-08) |
| Expense Core | GET | `/groups/{groupId}/schedules` | `listRecurringSchedules` | contract + controller/live smoke + non-member authorization + lifecycle persistence | production dependency-failure evidence (QA-08) |
| Expense Core | GET | `/groups/{groupId}/schedules/{scheduleId}` | `getRecurringSchedule` | contract + controller/live smoke + non-member authorization + unknown-resource 404 | production dependency-failure evidence (QA-08) |
| Expense Core | PUT | `/groups/{groupId}/schedules/{scheduleId}` | `updateRecurringSchedule` | contract + controller/live smoke + explicit principal enforcement + lifecycle update | production dependency-failure evidence (QA-08) |
| Expense Core | POST | `/groups/{groupId}/schedules/{scheduleId}/pause` | `pauseRecurringSchedule` | contract + persistence/controller lifecycle + live smoke + non-member authorization + idempotent paused replay | production dependency-failure evidence (QA-08) |
| Expense Core | POST | `/groups/{groupId}/schedules/{scheduleId}/resume` | `resumeRecurringSchedule` | contract + persistence/controller lifecycle + live smoke + explicit principal enforcement + idempotent resumed replay | production dependency-failure evidence (QA-08) |
| Notifications | GET | `/inbox` | `listInbox` | contract + persistence + live + auth/limit-boundary/cursor edges | failure/retry matrix |
| Notifications | POST | `/inbox/{notificationId}/read` | `markAsRead` | contract + persistence + live + authentication/malformed-ID/unknown-resource edges | failure/retry matrix |
| Notifications | GET | `/preferences` | `getPreferences` | contract + controller/live smoke + unauthenticated and blank-subject edges | persistence/failure matrix |
| Notifications | PUT | `/preferences` | `updatePreferences` | contract + controller/live smoke + unauthenticated and blank-subject edges | persistence/failure matrix |

## GraphQL and WebSocket operation matrix

Current provider-parity note: the focused BFF GraphQL transport suite is green,
and a rebuilt `local-oidc` Compose probe now forwards a real Keycloak bearer token
through BFF to Expense Core, returning HTTP 200 with an empty groups result.

| Surface | Operation | Current evidence | Remaining dimension work |
|---|---|---|---|
| GraphQL query | `me` | schema + resolver/controller + focused HTTP success + redacted upstream-timeout HTTP envelope | live authenticated BFF HTTP probe verified through the provider boundary; production retry/timeout policy evidence remains QA-08 |
| GraphQL query | `groups` | schema + resolver/controller + HTTP success + live empty-result journey + redacted timeout transport + live Expense Core outage envelope and recovery | production retry/timeout policy evidence (QA-08) |
| GraphQL query | `group` | schema + resolver/controller + HTTP success + live malformed-ID/not-found/non-member error envelopes + auth/validation/timeout/malformed-upstream transport tests | production retry policy evidence (QA-08) |
| GraphQL query | `settlementSuggestions` | schema + resolver/controller + HTTP success/empty/upstream-failure envelopes + live malformed-ID/non-member errors + live journey | production retry/timeout policy evidence (QA-08) |
| GraphQL mutation | `createGroup` | schema + resolver/controller + HTTP success + redacted conflict envelope + live malformed-enum validation/journey | production retry policy evidence (QA-08) |
| GraphQL mutation | `updateGroup` | schema + resolver/controller + HTTP success + validation/timeout error transport + live non-member authorization | production retry policy evidence (QA-08) |
| GraphQL mutation | `createExpense` | schema + resolver/controller + HTTP success + malformed-input/dependency-failure transport + live idempotent/tampered-replay edges + REST side-effect E2E | production retry policy evidence (QA-08) |
| GraphQL mutation | `recordRepayment` | schema + resolver/controller + HTTP success + conflict transport + live malformed-money/non-member authorization/journey | production retry policy evidence (QA-08) |
| GraphQL subscription | `groupChanged` | schema + resolver authorization + fanout unit tests + malformed-operation, authenticated/non-member, disconnect/reconnect, resubscription, and completed-subscription filtering WebSocket E2E + **SEC-01D: subscription auto-terminates on membership removal via revocation signals** | production replay/backpressure protocol evidence (QA-08) |
| WebSocket transport | `graphql-transport-ws` connection lifecycle | protocol handshake, subscribe, malformed-operation error frame, event delivery, authenticated resubscription after disconnect, and concurrent invalidation E2E | malformed frames, duplicate subscribe, timeout, and sustained backpressure evidence (QA-08) |

The local QA-07 package covers success, validation, authorization, upstream
error/timeout conversion, redaction, empty results, replay, and observable side
effects across every GraphQL HTTP operation. The remaining GraphQL/WebSocket
protocol dimensions (malformed frames, duplicate subscription IDs, sustained
backpressure, reconnect replay, and heartbeats) and production dependency
failure/retry policies are formally specified by contract and accepted limitations
in [`production-validation.md`](production-validation.md) under QA-08; local
Docker evidence and schema mocks are not presented as target-environment evidence.

## SEC-01 security finding closure summary

| Finding | Severity | Workstream | Code Evidence | Status |
|---|---|---|---|---|
| SEC-001: No deployed token issuance | Critical | SEC-01A | `AsymmetricJwtTokenProvider`, `OidcDiscoveryController`, production profile wiring | ✅ Verified closed |
| SEC-002: Email-derived identity | High | SEC-01B | `AccountIdentityEntity`, `JpaAccountIdentityStore`, `LoginVerificationService` | ✅ Verified closed |
| SEC-003: WebSocket membership not revoked | High | SEC-01D | `LiveUpdateFanout.revocationSignal`, `BffEventConsumer` member.removed handling | ✅ Verified closed |
| SEC-004: Profile IDOR | High | SEC-01C | `ProfileController` self/workload authorization, 403 on cross-account reads | ✅ Verified closed |
| SEC-005: Service-to-service trust | High | SEC-01C | `X-Squarewise-Workload-Role` header validation, workload-only batch route | ✅ Verified closed (code level; network policy remains release-gated) |
| SEC-006: CSRF incomplete | Medium | SEC-01D | `BrowserCsrfWebFilter` extended to `POST /graphql` cookie-authenticated mutations | ✅ Verified closed |
| SEC-007: Rate limit proxy sensitivity | Medium | SEC-01E | `ClientAddressResolver`, `TrustedProxyProperties`, IPv4/IPv6 normalization | ✅ Verified closed |
| SEC-008: Access-token revocation window | Medium | SEC-01E | 10-minute access-token lifetime documented; residual window explicitly accepted | ✅ Documented and accepted |
| SEC-009: External provider not implemented | Medium | SEC-01A / QA10-A04 | Squarewise-owned issuer is the verified default deployed authority; the optional external-provider property still selects an SPI placeholder that throws and must remain disabled or fail startup until a real exchange is implemented | ⚠️ Default path closed; explicitly enabled external path remains open |
| SEC-010: Key/secret rotation incomplete | Medium | SEC-01F | Key rotation runbook, overlap tests, `KEY_ROTATED` audit event | ✅ Runbook and tests complete; production rehearsal release-gated |
| SEC-011: Supply chain evidence partial | Medium | SEC-01F | Supply chain evidence doc, SBOM validation, hygiene scan, image hardening | ✅ Local CI evidence complete; container scanning release-gated |
| SEC-012: Security telemetry incomplete | Low/Medium | SEC-01F | `SecurityAuditLogger`, events on login/refresh/revoke/rotation | ✅ Implemented; immutable retention release-gated |
| SEC-013: Endpoint matrix incomplete | Low/Medium | SEC-01F | This document updated with SEC-01 evidence per operation | ✅ Updated; production dimensions remain QA-08 |
