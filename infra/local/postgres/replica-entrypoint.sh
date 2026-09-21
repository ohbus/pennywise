#!/usr/bin/env bash
set -euo pipefail

data_dir="${PGDATA:-/var/lib/postgresql/data/pgdata}"
primary_host="${POSTGRES_PRIMARY_HOST:?POSTGRES_PRIMARY_HOST is required}"
primary_port="${POSTGRES_PRIMARY_PORT:-5432}"
replication_user="${POSTGRES_REPLICATION_USER:?POSTGRES_REPLICATION_USER is required}"
replication_password="${POSTGRES_REPLICATION_PASSWORD:?POSTGRES_REPLICATION_PASSWORD is required}"

if [[ ! -s "${data_dir}/PG_VERSION" ]]; then
  mkdir -p "${data_dir}"
  chmod 700 "${data_dir}"
  export PGPASSWORD="${replication_password}"
  until pg_basebackup \
      --host="${primary_host}" \
      --port="${primary_port}" \
      --username="${replication_user}" \
      --pgdata="${data_dir}" \
      --write-recovery-conf \
      --progress; do
    sleep 2
  done
  unset PGPASSWORD
fi

exec docker-entrypoint.sh postgres -c hot_standby=on
