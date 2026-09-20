# Local release verification record

This checklist records evidence from the host-native local stack on 2026-09-19.
It is a pre-deployment verification record and does not authorize production
promotion.

- [x] All four applications start with authenticated REST/GraphQL flows; a real
  Keycloak client-credentials token was verified through BFF GraphQL to Expense
  Core after readiness-gated startup and centralized bearer propagation.
- [x] Docker full-stack startup is reproducible from host-built JARs without runtime Gradle downloads (`make package`, `docker compose ... build`, `up -d --wait`).
- [x] Accounts, Expense Core, Notifications, and BFF readiness probes pass.
- [x] Prometheus scrapes all four services; Grafana datasource and overview dashboard are provisioned.
- [x] Fixture-backed mutation load completed 250 writes with 0% failures and reconciled expenses, postings, outbox, sync, and revision counts via `make load-mutation-check`.
- [x] Duplicate expense replay is idempotent at the persistence-effect level without revision bumps; conflicting payloads return HTTP 409.
- [x] Full ordered Bruno collection executed 43 requests with the local environment and centralized token override.
- [x] Bruno response assertions cover identity, persistence, contract-shaped bodies, GraphQL errors, structured API errors, and authentication challenges.
- [x] A seeded RabbitMQ notification appeared in the test-user inbox and was marked read through the public Notifications API.
- [x] Recurring schedule create/list/get/update/pause/resume flow passed through the public Expense Core API.
- [x] Public acceptance journeys passed, including rollback, authorization, fanout, and recovery probes.
- [x] RabbitMQ loss/recovery restored the Notifications consumer in 11 seconds.
- [x] PostgreSQL loss/recovery and BFF upstream Accounts loss/recovery were exercised locally; the expected outage behavior and subsequent recovery were observed.
- [x] A 30-second mixed k6 run exercised the 1M profile concurrently with real mutation traffic: 0% HTTP failures, but Expense Core latency thresholds were exceeded under this local saturation level and the result is recorded as a capacity limitation.
- [x] Repository checks passed: `make check`, `make observability-validate`, `make security-hygiene`, `make load-k6-validate`, `make release-gate`, and contract validation.
- [ ] Production backup/restore, alert routing, secret rotation, ingress controls, image/license scanning, and rollback compatibility remain deployment gates.

Evidence is recorded in `docs/tasks/progress.md`; automated reconciliation verified via `tools/ops/reconcile_mutation_fixture.py`, Bruno output was written to
`/tmp/pennywise-bruno/assertions.json`, quality assertions to
`/tmp/pennywise-bruno/quality2.json`, and Prometheus/Grafana were verified on
ports 9090 and 3000. Local metrics and latency results must not be presented as
production capacity guarantees.
