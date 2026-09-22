# CQRS replica operations

The reader topology is optional. The default local Compose profile is
writer-only; merge `infra/local/docker-compose.replica.yml` to run the
diagnostic asynchronous PostgreSQL replica.

## Reader lag

`DbReaderLagProbe` reads PostgreSQL replay timestamp/LSN metadata from the
reader pool. A reader above the configured `pennywise.db.reader-lag-budget-ms`
is marked `LAGGING`, and eventual queries may use the bounded writer fallback.
Strong, command, lock, claim, migration, and reconciliation operations never
use that fallback policy.

## Reader outage

Connection/probe failures transition the named reader through disconnected and
circuit-open states. Inspect `pennywise_db_reader_failure_total`, the service
health endpoint, and the application route counters. Restore the reader or
force writer-only mode by setting `PENNYWISE_DB_ENABLED=false` and restarting
the affected service. Do not delete PostgreSQL volumes as an outage response.

## Writer fallback

Fallback is bounded to explicitly approved eventual queries. Sustained
`pennywise_db_fallback_total` growth is an incident signal because it can
consume writer capacity. Investigate reader lag/pool exhaustion and disable
replica routing before increasing writer pool limits.

## Evidence boundary

`sh tests/performance/cqrs-replica-smoke.sh` proves only local streaming,
recovery mode, replay position, and authenticated route telemetry. It is not
production capacity, failover, backup/restore, durability, or alert-routing
proof.
