#!/usr/bin/env sh
set -eu

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
cd "$repo_root"
python3 tools/contracts/validate.py
docker compose -f infra/local/docker-compose.yml config --quiet
docker compose -f infra/local/docker-compose.dev.yml config --quiet
PENNYWISE_ACCOUNTS_IMAGE=example/accounts:local \
PENNYWISE_EXPENSE_CORE_IMAGE=example/expense-core:local \
PENNYWISE_NOTIFICATIONS_IMAGE=example/notifications:local \
PENNYWISE_BFF_IMAGE=example/bff:local \
docker compose -f infra/deploy/docker-compose.prod.yml config --quiet
printf '%s\n' "contract and deployment smoke checks passed"
