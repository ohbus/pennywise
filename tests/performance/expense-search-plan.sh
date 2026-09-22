#!/usr/bin/env sh
set -eu

COMPOSE_ARGS="-f ${COMPOSE_BASE:-infra/local/docker-compose.yml} -f ${COMPOSE_DEV:-infra/local/docker-compose.dev.yml} -f ${COMPOSE_REPLICA:-infra/local/docker-compose.replica.yml}"
GROUP_ID=${GROUP_ID:-00000000-0000-0000-0000-000000000000}

plan=$(docker compose $COMPOSE_ARGS exec -T postgres-replica psql -U pennywise -d pennywise_expense_core -X -qAt -c "EXPLAIN (ANALYZE, BUFFERS, SETTINGS) SELECT CAST(e.expense_id AS VARCHAR) AS expense_id, e.description, e.category, e.currency, e.amount_minor FROM expenses e WHERE e.group_id = '$GROUP_ID' AND e.deleted = FALSE AND ('dinner' = '' OR LOWER(e.description) LIKE CONCAT('%', LOWER('dinner'), '%')) ORDER BY e.expense_id ASC LIMIT 101;")
printf '%s\n' "$plan"
printf '%s\n' "$plan" | grep -q 'Index Scan using expenses_group_idx'
printf '%s\n' 'expense_search_plan=indexed_group_scan'
