#!/usr/bin/env sh
set -eu
COMPOSE_FILE=${COMPOSE_FILE:-infra/local/docker-compose.yml}; SERVICE=${SERVICE:-expense-core}; TIMEOUT_SECONDS=${TIMEOUT_SECONDS:-120}
docker compose -f "$COMPOSE_FILE" config --quiet
docker compose -f "$COMPOSE_FILE" ps
docker compose -f "$COMPOSE_FILE" restart "$SERVICE"
deadline=$(( $(date +%s) + TIMEOUT_SECONDS ))
while :; do
  if docker compose -f "$COMPOSE_FILE" ps --status running --services | grep -qx "$SERVICE"; then echo "service=$SERVICE recovery=running"; exit 0; fi
  [ "$(date +%s)" -lt "$deadline" ] || { echo "service=$SERVICE recovery=timeout" >&2; exit 1; }
  sleep 2
done
