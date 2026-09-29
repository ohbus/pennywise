# Key rotation and incident operations runbook (SEC-010)

## Scope

This runbook covers rotating the Squarewise-owned asymmetric RSA signing key pair,
rotating the credential-digest HMAC secret, rotating the auth-email envelope encryption
key, and the emergency global session revocation procedure. All rotation events must
produce a structured audit log entry (`security.audit` logger at INFO level).

---

## 1. RSA signing key rotation

The `DefaultRsaKeyProvider` supports zero-downtime rotation with an overlapping public-key
verification window. All previous public keys remain in the JWKS until explicitly retired,
so resource servers can continue verifying tokens signed with the previous key while
the new key propagates through their JWKS caches.

### Pre-rotation checklist

- [ ] Generate the replacement key pair out-of-band (2048-bit RSA minimum; 4096-bit recommended
  for long-lived deployment keys).
- [ ] Record the new `kid` in the key inventory with rotation timestamp and operator identity.
- [ ] Confirm that all resource-server JWKS cache TTLs are known (default: 5 minutes in
  Spring Security's `NimbusJwtDecoder` cache).
- [ ] Notify the on-call team before starting the window.

### Rotation procedure

1. Mount the new key pair into the running Accounts service via the configured secret manager
   (environment variables `SQUAREWISE_SECURITY_JWT_PUBLIC_KEY_PEM` /
   `SQUAREWISE_SECURITY_JWT_PRIVATE_KEY_PEM` / `SQUAREWISE_SECURITY_JWT_KEY_ID`).
2. Call `DefaultRsaKeyProvider.rotateKey(newKey)` (exposed via a dedicated internal admin
   endpoint, or by performing a rolling restart of the Accounts container with the new key
   variables; the provider reloads the key on startup).
3. Verify the JWKS endpoint returns **both** the old `kid` and new `kid`:
   ```
   curl https://accounts.internal/.well-known/jwks.json | jq '.keys[].kid'
   ```
4. Wait for one JWKS cache TTL cycle (5 minutes) so that all resource servers have picked
   up the new key.
5. Issue a test token and verify it validates at Accounts, Expense Core, Notifications, and BFF.
6. Confirm the `security.audit` log contains `event=KEY_ROTATED kid=<new-kid>`.

### Post-rotation cleanup (after overlap window)

The overlap window duration must exceed the maximum access-token lifetime (10 minutes) plus
the resource-server JWKS cache TTL (5 minutes). Do not retire the old public key before
**15 minutes** after the rotation is confirmed at all services.

To retire an old public key, redeploy the Accounts service without the retired kid in the
historical set, or explicitly implement a retirement API on `DefaultRsaKeyProvider`. Do not
remove a key that tokens in active circulation may still need for verification.

### Rollback

If the new key cannot be verified at any resource server within 5 minutes:

1. Revert the Accounts service to the previous key configuration (rolling restart).
2. The old key is already in the JWKS historical set; no tokens are invalidated.
3. Record the failed rotation attempt in the security incident log.

---

## 2. Credential-digest HMAC secret rotation

The `HmacCredentialDigest` uses a secret key to hash one-time login credentials at rest.
Rotating this secret invalidates any in-flight login links or codes that have not yet been
redeemed.

### Rotation procedure

1. Determine the active credential lifetime (default: 15 minutes from `LoginCredentialService`).
2. Wait for active credentials to expire, **or** accept that pending links will be invalidated
   (users must request a new link after rotation).
3. Update the `SQUAREWISE_SECURITY_CREDENTIAL_DIGEST_SECRET` environment variable in the
   secrets manager.
4. Perform a rolling restart of the Accounts service.
5. Verify that a fresh login start/verify cycle succeeds end-to-end.

### Emergency rotation (credential compromise suspected)

If a plaintext digest secret is suspected to have been leaked:

1. Rotate immediately following the procedure above, accepting link invalidation.
2. Revoke all active auth sessions (`auth_sessions`) where the compromise window overlaps
   (see §4 emergency revocation).
3. Record the incident with the compromise window, affected credential count (if estimable),
   and operator identity.

---

## 3. Auth-email envelope key rotation

The auth-email delivery uses an AES-GCM authenticated encryption envelope. Rotating this key
does not invalidate issued credentials (they are stored as digests, not ciphertext), but
in-transit outbox entries with the old envelope will fail to decrypt if the key is changed
before they are delivered.

### Rotation procedure

1. Drain the `auth_email_outbox` table to empty before rotating (wait for the outbox relay
   to confirm delivery of all pending entries).
2. Update `SQUAREWISE_SECURITY_AUTH_EMAIL_ENVELOPE_KEY` in the secrets manager.
3. Perform a rolling restart of the Accounts service.
4. Verify that a fresh login start triggers a new outbox entry and that the email is
   delivered successfully.

---

## 4. Emergency global session revocation

Use this procedure when a signing key compromise, refresh-secret compromise, or account
takeover requires bulk invalidation of all or a subset of active sessions.

### Full account revocation

Use the `TokenSessionService.revokeSessionByRefreshToken` or `revokeFamily` paths
triggered through the authorized administrative operation. A `SESSION_REVOKED` event
will be emitted to the `security.audit` log for each family.

For bulk revocation of all sessions for a single account:
```sql
-- Run as the accounts-rw writer role only; log the operator and timestamp
UPDATE auth_sessions
   SET revoked_at = now()
 WHERE account_id = '<target-account-id>'
   AND revoked_at IS NULL;
```

### Global emergency revocation (all sessions)

Only for complete signing-key or database-credential compromise:
```sql
-- Requires DBA approval and incident log entry
UPDATE auth_sessions
   SET revoked_at = now()
 WHERE revoked_at IS NULL;
```

After bulk revocation:
1. Rotate the affected secrets immediately.
2. Send incident notifications to all affected accounts per the privacy/breach policy.
3. Record the event in the immutable security incident log with: incident ID, operator,
   timestamp, scope, and root cause.

---

## 5. Verification after any rotation

Run the following commands to confirm system health after each rotation:

```bash
# Confirm JWKS contains expected keys
curl https://accounts.internal/.well-known/jwks.json | jq '.keys[].kid'

# Confirm audit log emitted rotation event
# (Check the security.audit log stream for the last 15 minutes)

# Run the accounts test suite for regression
./gradlew :app:accounts:test --no-daemon

# Confirm supply chain and hygiene gates pass
uv run python tools/ops/check_security_hygiene.py
uv run python tools/ops/validate_sbom_baseline.py
```

---

## 6. Operator authorization and audit requirements

- All rotation operations must be authorized by at least one designated security operator.
- The `security.audit` log stream must be retained in an immutable sink (e.g., CloudWatch
  Logs with object lock, or equivalent) for a minimum of 90 days.
- Rotation events must include: event type, `kid` (for key rotation), timestamp, and
  operator identity (recorded out-of-band in the incident log, not in the application log).
- All runbook executions must be recorded in the incident/change management system.
