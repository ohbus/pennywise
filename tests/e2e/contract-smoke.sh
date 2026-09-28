#!/usr/bin/env sh
set -eu

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
cd "$repo_root"
python3 tools/contracts/validate.py
docker compose -f infra/local/docker-compose.yml config --quiet
PENNYWISE_SECURITY_CREDENTIAL_DIGEST_SECRET="${PENNYWISE_SECURITY_CREDENTIAL_DIGEST_SECRET:-AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=}" \
PENNYWISE_SECURITY_AUTH_EMAIL_ENVELOPE_KEY="${PENNYWISE_SECURITY_AUTH_EMAIL_ENVELOPE_KEY:-ICEiIyQlJicoKSorLC0uLzAxMjM0NTY3ODk6Ozw9Pj8=}" \
docker compose -f infra/local/docker-compose.dev.yml config --quiet
PENNYWISE_ACCOUNTS_IMAGE=example/accounts:local \
PENNYWISE_EXPENSE_CORE_IMAGE=example/expense-core:local \
PENNYWISE_NOTIFICATIONS_IMAGE=example/notifications:local \
PENNYWISE_BFF_IMAGE=example/bff:local \
docker compose -f infra/deploy/docker-compose.prod.yml config --quiet
printf '%s\n' "contract and deployment smoke checks passed"
