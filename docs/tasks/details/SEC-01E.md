# SEC-01E — Formalize rate-limit and bearer-revocation guarantees

## Status

Planned. Depends on SEC-01A and SEC-01B.

## Objective

Resolve findings SEC-007 (rate-limit client IP extraction) and SEC-008 (access-token
revocation residual window).

1. **Abuse Control Client Address Resolution (SEC-007):**
   - Implement canonical client IP extraction that respects an explicit trusted-proxy
     configuration rather than blindly parsing forwarded headers.
   - Replace the two-octet IPv4 truncation heuristic with a proper subnet/IP hashing
     mechanism supporting both IPv4 and IPv6.
   - Prevent header spoofing from untrusted downstreams.
2. **Access-Token Revocation Semantics (SEC-008):**
   - Bound the access-token residual lifetime window (short TTL: 5-15 minutes).
   - Ensure refresh session family revocation in PostgreSQL immediately denies any
     new token issuance.
   - Document the revocation guarantee model and add validation for high-risk operations
     and session invalidation.

## Owned Paths

- `app/accounts/src/main/kotlin/com/squarewise/accounts/auth/abuse/`
- `app/accounts/src/main/kotlin/com/squarewise/accounts/auth/session/`
- `docs/tasks/details/SEC-01E.md`

## Acceptance Criteria

1. Client IP resolution parses client addresses only from configured trusted proxies;
   direct untrusted connections use remote socket address.
2. Rate-limit keys partition IPv4 and IPv6 addresses without accidental collisions across
   unrelated /16 subnets.
3. Access token lifetime is explicitly bounded and documented.
4. Comprehensive test coverage for direct connections, proxy chains, spoofed headers,
   IPv6 addresses, and token expiry boundaries.

## Verification Commands

- `./gradlew :app:accounts:test --rerun-tasks --no-daemon`
- `uv run python tools/contracts/validate.py`
- `uv run python tools/ops/check_security_hygiene.py`
