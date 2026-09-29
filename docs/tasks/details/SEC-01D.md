# SEC-01D — Close browser mutation and subscription time-of-check gaps

## Status

Planned. Depends on SEC-01B.

## Objective

Resolve findings SEC-003 (WebSocket continuous authorization) and SEC-006 (browser
mutation CSRF protection).

1. **WebSocket Continuous Authorization (SEC-003):**
   - Eliminate time-of-check to time-of-use vulnerability in GraphQL WebSocket
     subscriptions (`GroupGraphqlController`).
   - Group event fanout validates caller membership continuity before emitting events
     to active subscribers.
   - When a member is removed from a group or an account/session is revoked, the
     subscription stream is terminated or suppressed.
2. **Browser Mutation CSRF Protection (SEC-006):**
   - Extend the double-submit CSRF nonce and origin enforcement in `BrowserCsrfWebFilter`
     to cover cookie-authenticated state-changing GraphQL mutations.
   - Pure bearer token requests (native/CLI) bypass CSRF checks cleanly.
   - Requests with access cookies lacking valid CSRF headers are rejected with 403 Forbidden
     before mutation resolvers execute.

## Owned Paths

- `app/bff/src/main/kotlin/com/squarewise/bff/transport/graphql/GroupGraphqlController.kt`
- `app/bff/src/main/kotlin/com/squarewise/bff/transport/http/BrowserCsrfWebFilter.kt`
- `app/bff/src/main/kotlin/com/squarewise/bff/transport/http/BrowserOriginWebFilter.kt`
- `app/bff/src/main/kotlin/com/squarewise/bff/transport/http/BrowserAccessCookieWebFilter.kt`
- `docs/tasks/details/SEC-01D.md`

## Acceptance Criteria

1. An established WebSocket subscription terminates or suppresses events as soon as
   the subscriber loses group membership.
2. GraphQL mutations called with cookie authentication require the CSRF double-submit
   header (`X-CSRF-Token` matching `SW_CSRF`).
3. GraphQL queries or bearer-authenticated requests remain unaffected.
4. Comprehensive tests cover midstream revocation, CSRF mismatch, missing token,
   and allowed bearer mutations.

## Verification Commands

- `./gradlew :app:bff:test --rerun-tasks --no-daemon`
- `uv run python tools/contracts/validate.py`
- `uv run python tools/ops/check_security_hygiene.py`
