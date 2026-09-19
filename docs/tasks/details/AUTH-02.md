# AUTH-02 — Remove implicit authentication identities

## Objective

Remove every runtime fallback that turns an absent or blank authenticated
principal into a synthetic user such as `test-user`. Authentication failures
must be explicit, structured, side-effect-free, and consistent across all
Expense Core expense operations.

## Dependencies and ownership

- Depends on `AUTH-01` and the structured security error behavior from `ERR-03`.
- Owns the Expense Core expense controller path and its focused tests, plus the
  required REST-edge, Bruno, and live acceptance evidence.
- Does not redesign the OIDC provider or add Keycloak; those are later tasks.

## Implementation requirements

1. Require a non-null, non-blank principal subject before membership lookup.
2. Return the existing structured unauthenticated error code/status used by the
   service; do not leak membership or group existence information.
3. Ensure no repository, expense store, audit, outbox, or balance operation is
   called after authentication rejection.
4. Preserve existing authenticated-member, non-member, archived-group, and
   malformed-request behavior.
5. Remove or update test-only argument resolvers that hide missing-principal
   behavior; successful tests must provide an explicit principal.

## Required tests

### Unit/controller

- create, update, delete, list, and balances reject a missing principal;
- blank and whitespace-only subjects reject identically;
- rejected requests do not invoke membership or expense persistence;
- authenticated member behavior remains green;
- non-member and archived-group behavior remains unchanged.

### REST/E2E

- REST edge suite probes every affected operation without `Authorization`;
- live suite verifies HTTP status, structured error code, and no side effect;
- authenticated and non-member control cases remain covered.

### Bruno

Add or update unauthenticated requests for every affected operation and assert
status, problem content type, stable error code, request ID, and absence of a
domain response. Use the central environment token only for authenticated
control cases.

## Documentation updates required

- Update this task with implementation notes and exact evidence.
- Update `docs/tasks/progress.md` with command results and commit hash.
- Update API/error documentation if response details change.
- Update the security hardening plan if a later provider integration depends on
  the new subject requirement.

## Acceptance criteria

- No `test-user` or equivalent fallback remains in runtime authentication paths.
- All five affected expense operations reject absent/blank identity with the
  structured unauthenticated response.
- No rejected request reaches a membership or financial store.
- Focused unit tests, REST-edge E2E tests, Bruno, and live acceptance evidence
  pass, or environment limitations are explicitly recorded.
- The change is committed as one coherent increment with a clean worktree.

## Status

Planned. No implementation is claimed until the required tracker, tests, and
evidence are updated.
