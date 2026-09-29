# SEC-01F — Make security operations, telemetry, supply chain, and release evidence executable

## Status

Planned. Consumes evidence from SEC-01A through SEC-01E.

## Objective

Resolve findings SEC-010 (secrets/key rotation), SEC-011 (supply chain and deployment),
SEC-012 (security telemetry & audit trails), and SEC-013 (endpoint matrix completeness).

1. **Key Rotation & Incident Operations (SEC-010):**
   - Provide runbooks and verification tests for asymmetric signing key rotation,
     credential hashing secret rotation, and session invalidation.
2. **Supply Chain & Deployment Hardening (SEC-011):**
   - Verify SBOM generation, dependency vulnerability checks, action pinning, and
     deployment configuration hardening.
3. **Security Telemetry & Structured Audit Events (SEC-012):**
   - Emit sanitized security events for authentication lifecycle, token refresh replay,
     authorization failures, and administrative actions without logging secrets or PII.
4. **Endpoint Matrix & Production Readiness Evidence (SEC-013):**
   - Reconcile `docs/quality/public-interface-operation-matrix.md` with complete evidence
     for authentication, authorization, and failure modes across all 45+ endpoints.
   - Evaluate lifting of the public production NO-GO gate upon complete verification.

## Owned Paths

- `docs/operations/`
- `docs/security/`
- `docs/quality/`
- `.github/workflows/`
- `tools/ops/`
- `docs/tasks/details/SEC-01F.md`

## Acceptance Criteria

1. Asymmetric key rotation procedures are documented and verified with overlap tests.
2. Structured security audit events are logged for key security events without secret leakage.
3. Supply chain hygiene checks pass cleanly (`check_security_hygiene.py`, `validate_sbom_baseline.py`).
4. Endpoint operation matrix is updated with verified evidence across all security dimensions.
5. All P0 and P1 security findings from the whole-security audit are verified closed.

## Verification Commands

- `./gradlew test check --no-daemon`
- `uv run python tools/contracts/validate.py`
- `uv run python tools/ops/check_security_hygiene.py`
- `uv run python tools/ops/check_architecture.py`
- `uv run python tools/ops/validate_sbom_baseline.py`
