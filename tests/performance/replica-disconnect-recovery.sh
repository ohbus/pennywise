#!/usr/bin/env sh
set -eu

COMPOSE_ARGS="-f ${COMPOSE_BASE:-infra/local/docker-compose.yml} -f ${COMPOSE_DEV:-infra/local/docker-compose.dev.yml} -f ${COMPOSE_REPLICA:-infra/local/docker-compose.replica.yml}"
PRIMARY_SERVICE=${PRIMARY_SERVICE:-postgres}
REPLICA_SERVICE=${REPLICA_SERVICE:-postgres-replica}

compose() { # shellcheck disable=SC2086
  docker compose $COMPOSE_ARGS "$@"
}

restore_replica() {
  compose start "$REPLICA_SERVICE" >/dev/null 2>&1 || true
}
trap restore_replica EXIT

compose config --quiet
compose stop "$REPLICA_SERVICE" >/dev/null
writer_status=$(compose exec -T "$PRIMARY_SERVICE" pg_isready -U pennywise -d postgres)
printf '%s\n' "$writer_status"
printf '%s\n' 'replica_disconnect=writer_healthy'

compose start "$REPLICA_SERVICE" >/dev/null
compose up -d --wait "$REPLICA_SERVICE" >/dev/null
streaming=$(compose exec -T "$PRIMARY_SERVICE" psql -U pennywise -d postgres -tAc "SELECT COALESCE(bool_and(state = 'streaming'), false) FROM pg_stat_replication;" | tr -d '[:space:]')
recovery=$(compose exec -T "$REPLICA_SERVICE" psql -U pennywise -d postgres -tAc "SELECT pg_is_in_recovery();" | tr -d '[:space:]')
printf 'replica_recovery_streaming=%s\n' "$streaming"
printf 'replica_recovery_mode=%s\n' "$recovery"
[ "$streaming" = true ] || [ "$streaming" = t ]
[ "$recovery" = true ] || [ "$recovery" = t ]
