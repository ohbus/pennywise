#!/usr/bin/env sh
set -eu
db=${DB_MONTHLY_EUR:-0}; broker=${BROKER_MONTHLY_EUR:-0}; node=${APP_NODE_MONTHLY_EUR:-0}; nodes=${APP_NODES:-1}; ops=${OPS_MONTHLY_EUR:-0}
total=$((db + broker + node * nodes + ops))
printf '%s\n' "db_monthly_eur=$db" "broker_monthly_eur=$broker" "app_node_monthly_eur=$node" "app_nodes=$nodes" "ops_monthly_eur=$ops" "estimated_total_monthly_eur=$total" "basis=operator-supplied assumptions"
