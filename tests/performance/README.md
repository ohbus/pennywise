# OPS-02 performance and recovery drills

These scripts are reproducible probes for an isolated Pennywise environment.
They do not provision infrastructure or claim production capacity.

```sh
BASE_URL=http://localhost:8080 REQUESTS=50 CONCURRENCY=5 tests/performance/capacity-smoke.sh
COMPOSE_FILE=infra/local/docker-compose.yml tests/performance/recovery-drill.sh
tests/performance/cost-estimate.sh
```

The capacity probe checks readiness and reports throughput and failures. Supply
`AUTH_HEADER` and `HEALTH_PATH` to probe an authenticated endpoint. The recovery
drill validates Compose, restarts one service, and waits for it to run. It does
not delete volumes or test backup restoration. A launch restore drill must use
an isolated PostgreSQL backup, reconcile balances, audit rows, and outbox replay.

The cost output is an arithmetic estimate from operator supplied assumptions,
not a provider quote. Set `DB_MONTHLY_EUR`, `BROKER_MONTHLY_EUR`,
`APP_NODE_MONTHLY_EUR`, `APP_NODES`, and `OPS_MONTHLY_EUR`.
