# SEC-01C — Enforce object authorization and workload trust

## Status

Planned. Depends on SEC-01A and SEC-01B.

## Objective

Resolve findings SEC-004 (profile object authorization) and SEC-005 (service-to-service
workload trust).

1. **Profile Object Authorization (SEC-004):**
   - Public profile access is restricted to the caller's own profile (`/me`).
   - Arbitrary account lookups via `GET /profiles/{accountId}` and `POST /profiles/batch`
     require either verified co-membership in an authorized context or a dedicated
     internal workload identity.
   - Unauthorized reads fail with 403 Forbidden / 404 Not Found rather than disclosing
     profile DTOs to arbitrary authenticated subjects.
2. **Workload Trust Boundary (SEC-005):**
   - Service-to-service calls between BFF and backend services use distinct workload
     credentials, scopes, or dedicated internal endpoints.
   - Services do not rely on private Docker networking alone for authorization.
   - Cross-service user token propagation includes verification of audience and
     required claims.

## Owned Paths

- `app/accounts/src/main/kotlin/com/squarewise/accounts/profile/api/ProfileController.kt`
- `app/accounts/src/main/kotlin/com/squarewise/accounts/profile/`
- `app/bff/src/main/kotlin/com/squarewise/bff/`
- `contracts/rest/accounts.openapi.json`
- `docs/tasks/details/SEC-01C.md`

## Acceptance Criteria

1. `ProfileController` rejects attempts by User A to read User B's profile via
   `GET /profiles/{id}` unless authorized by shared context or internal workload role.
2. `POST /profiles/batch` filters or rejects unauthorized IDs.
3. Updated OpenAPI contracts reflect the secured profile endpoints and error shapes.
4. Internal service calls authenticate with appropriate workload roles/credentials.
5. Unit and integration tests verify rejection of cross-account reads, anonymous calls,
   and untrusted workload requests.

## Verification Commands

- `./gradlew :app:accounts:test --rerun-tasks --no-daemon`
- `./gradlew :app:bff:test --rerun-tasks --no-daemon`
- `uv run python tools/contracts/validate.py`
- `uv run python tools/contracts/validate_public_surface.py`
- `uv run python tools/ops/check_security_hygiene.py`
