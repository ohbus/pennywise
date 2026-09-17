# Operations plan

The first environment is a reproducible Docker Compose stack. It contains one
PostgreSQL cluster with separate service databases, RabbitMQ, and SMTP capture.
OIDC provider selection and provisioning remain external to the current local
topology. Testcontainers provides isolated dependencies for tests; tests never
depend on a developer's persistent Compose state.

Local development has explicit Compose entry points instead of requiring the
largest stack for every task. See [Compose topology](compose-topology.md) for the
dependency-only stack, each native application's prerequisites, the complete
containerized stack, ports, credentials, health checks, and matching Make
commands. [Clone and run](quickstart.md) is the shortest path for a new checkout;
the repository-wide command catalogue is always available through `make help`.

Production begins as stateless pinned containers behind a load balancer, with
managed PostgreSQL and RabbitMQ where possible. The deployment must provide TLS,
private service networking, scoped secrets, health/readiness endpoints, graceful
shutdown, and expand/contract migrations. A migration runs before an application
rollout and rollback uses a previously tested image digest.

Observe request success/latency, database pool saturation, outbox age, queue
lag/retries, dead letters, sync resets, subscription counts, recurring backlog,
and reconciliation results. Never put descriptions, tokens, invitation secrets,
or raw financial values into routine logs or metric labels.

Backups and restoration are launch gates. Restore into an isolated environment,
reconcile every group/currency, and verify outbox replay is duplicate-safe. Load
testing must define operation mix, hot groups, reconnect storms, and cost; one
million monthly users is an ambition, not evidence of capacity.

The production reference at `infra/deploy/docker-compose.prod.yml` is intended
for a private network behind an external TLS/load-balancing layer. It checks
`/actuator/health/readiness` before traffic, emits structured logs, forwards
termination signals, and allows graceful shutdown. Inject separate,
least-privilege credentials per service through the deployment platform; never
commit certificates, tokens, passwords, or broker credentials.

Run expand migrations before image rollout and keep the old immutable digest
available for rollback. Compose documents the topology but does not provision
cloud resources or managed PostgreSQL, RabbitMQ, TLS, or OTLP infrastructure.
