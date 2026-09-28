#!/usr/bin/env bash
# acquire_local_tokens.sh - Acquire ephemeral signed local OIDC tokens from Keycloak for E2E tests
set -euo pipefail

# Ensure keycloak container is reachable
TOKEN_ENDPOINT="http://idp-keycloak:8080/realms/squarewise/protocol/openid-connect/token"
RESOLVE_FLAG=("--connect-to" "idp-keycloak:8080:127.0.0.1:8090")

fetch_token() {
  local client_id="$1"
  local client_secret="$2"
  local realm="${3:-squarewise}"
  local endpoint="http://idp-keycloak:8080/realms/${realm}/protocol/openid-connect/token"

  local response
  response="$(curl --fail-with-body --silent --show-error \
    "${RESOLVE_FLAG[@]}" \
    --data "grant_type=client_credentials&client_id=${client_id}&client_secret=${client_secret}" \
    "${endpoint}")"
  python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])' <<< "$response"
}

TOKEN="$(fetch_token squarewise-ci squarewise-ci-local-only)"
test -n "$TOKEN" || { echo "Keycloak returned no primary access token" >&2; exit 2; }

BOB_TOKEN="$(fetch_token squarewise-ci-e2e-bob squarewise-ci-e2e-bob-local-only)"
test -n "$BOB_TOKEN" || { echo "Keycloak returned no E2E second-persona token" >&2; exit 2; }

NONMEMBER_TOKEN="$(fetch_token squarewise-ci-e2e-nonmember squarewise-ci-e2e-nonmember-local-only)"
test -n "$NONMEMBER_TOKEN" || { echo "Keycloak returned no E2E non-member token" >&2; exit 2; }

WRONG_AUDIENCE_TOKEN="$(fetch_token squarewise-ci-wrong-audience squarewise-ci-wrong-audience-local-only)"
test -n "$WRONG_AUDIENCE_TOKEN" || { echo "Keycloak returned no negative-audience token" >&2; exit 2; }

WRONG_ISSUER_TOKEN="$(fetch_token squarewise-ci-wrong-issuer squarewise-ci-wrong-issuer-local-only squarewise-other)"
test -n "$WRONG_ISSUER_TOKEN" || { echo "Keycloak returned no negative-issuer token" >&2; exit 2; }

# Optional: mint expired token if requested by setting INCLUDE_EXPIRED=true
EXPIRED_TOKEN=""
if [ "${INCLUDE_EXPIRED:-false}" = "true" ]; then
  docker compose -f infra/local/docker-compose.dev.yml exec -T idp-keycloak \
    /opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 \
    --realm master --user local-admin --password local-admin-only
  docker compose -f infra/local/docker-compose.dev.yml exec -T idp-keycloak \
    /opt/keycloak/bin/kcadm.sh update realms/squarewise -s accessTokenLifespan=1
  EXPIRED_TOKEN="$(fetch_token squarewise-ci squarewise-ci-local-only)"
  docker compose -f infra/local/docker-compose.dev.yml exec -T idp-keycloak \
    /opt/keycloak/bin/kcadm.sh update realms/squarewise -s accessTokenLifespan=300
  # Spring's default bounded clock skew is 60 seconds; wait beyond it.
  sleep 65
fi

# Export or output to GITHUB_OUTPUT / env file
if [ -n "${GITHUB_OUTPUT:-}" ]; then
  echo "token=${TOKEN}" >> "$GITHUB_OUTPUT"
  echo "e2e_bob_token=${BOB_TOKEN}" >> "$GITHUB_OUTPUT"
  echo "e2e_nonmember_token=${NONMEMBER_TOKEN}" >> "$GITHUB_OUTPUT"
  echo "wrong_audience_token=${WRONG_AUDIENCE_TOKEN}" >> "$GITHUB_OUTPUT"
  echo "wrong_issuer_token=${WRONG_ISSUER_TOKEN}" >> "$GITHUB_OUTPUT"
  if [ -n "${EXPIRED_TOKEN}" ]; then
    echo "expired_token=${EXPIRED_TOKEN}" >> "$GITHUB_OUTPUT"
  fi
fi

if [ -n "${GITHUB_ENV:-}" ]; then
  echo "BEARER_TOKEN=${TOKEN}" >> "$GITHUB_ENV"
  echo "SQUAREWISE_E2E_TOKEN_A=${TOKEN}" >> "$GITHUB_ENV"
  echo "SQUAREWISE_E2E_TOKEN_B=${BOB_TOKEN}" >> "$GITHUB_ENV"
  echo "SQUAREWISE_E2E_TOKEN_NONMEMBER=${NONMEMBER_TOKEN}" >> "$GITHUB_ENV"
  echo "WRONG_ISSUER_TOKEN=${WRONG_ISSUER_TOKEN}" >> "$GITHUB_ENV"
  echo "SQUAREWISE_BRUNO_TOKEN=${TOKEN}" >> "$GITHUB_ENV"
  echo "SQUAREWISE_BRUNO_WRONG_AUDIENCE_TOKEN=${WRONG_AUDIENCE_TOKEN}" >> "$GITHUB_ENV"
  echo "SQUAREWISE_BRUNO_WRONG_ISSUER_TOKEN=${WRONG_ISSUER_TOKEN}" >> "$GITHUB_ENV"
  if [ -n "${EXPIRED_TOKEN}" ]; then
    echo "EXPIRED_TOKEN=${EXPIRED_TOKEN}" >> "$GITHUB_ENV"
    echo "SQUAREWISE_BRUNO_EXPIRED_TOKEN=${EXPIRED_TOKEN}" >> "$GITHUB_ENV"
  fi
fi

echo "Successfully acquired local OIDC tokens."
