# Local Compose topology

All local entry points derive services from `infra/local/docker-compose.yml` or
`infra/local/docker-compose.dev.yml`. Local credentials are intentionally fixed
and must never be reused outside a developer machine. Run one topology at a
time because each publishes the same stable host ports.

## Command and service matrix

| Purpose | Compose file | Services | Start | Inspect | Stop |
| --- | --- | --- | --- | --- | --- |
| Shared dependencies | `infra/local/docker-compose.yml` | PostgreSQL, RabbitMQ, Mailpit | `make deps-up` | `make deps-status`, `make deps-logs` | `make deps-down` |
| Native Accounts | `infra/local/docker-compose.accounts.yml` | PostgreSQL | `make accounts-deps-up` | `make accounts-deps-status`, `make accounts-deps-logs` | `make accounts-deps-down` |
| Native Expense Core | `infra/local/docker-compose.expense-core.yml` | PostgreSQL, RabbitMQ | `make expense-core-deps-up` | `make expense-core-deps-status`, `make expense-core-deps-logs` | `make expense-core-deps-down` |
| Native Notifications | `infra/local/docker-compose.notifications.yml` | PostgreSQL, RabbitMQ, Mailpit | `make notifications-deps-up` | `make notifications-deps-status`, `make notifications-deps-logs` | `make notifications-deps-down` |
| Native BFF | `infra/local/docker-compose.bff.yml` | Accounts and Expense Core upstreams, PostgreSQL, RabbitMQ | `make bff-deps-up` | `make bff-deps-status`, `make bff-deps-logs` | `make bff-deps-down` |
| Complete stack | `infra/local/docker-compose.dev.yml` | All four applications plus PostgreSQL, RabbitMQ, Mailpit | `make full-up` | `make full-status`, `make full-logs` | `make full-down` |

Every row also has a `-config` target. `make compose-config` validates all six
files without starting containers. `make help` lists these commands and the
exact Compose file each command uses.

The BFF is database-free. Its standalone topology therefore starts its real
Accounts and Expense Core HTTP upstreams rather than PostgreSQL for the BFF
itself. Those upstreams require PostgreSQL, and Expense Core also requires
RabbitMQ. Notifications is not an implemented BFF gateway dependency and is not
included in this narrow topology.

## Ports and local credentials

| Component | Container port | Host port | Local access |
| --- | ---: | ---: | --- |
| PostgreSQL | 5432 | 5432 | user `pennywise`, password `pennywise-local-only` |
| RabbitMQ AMQP | 5672 | 5672 | user `pennywise`, password `pennywise-local-only` |
| RabbitMQ management | 15672 | 15672 | `http://localhost:15672` with the RabbitMQ local credentials |
| Mailpit SMTP | 1025 | 1025 | no authentication |
| Mailpit web UI | 8025 | 8025 | `http://localhost:8025` |
| BFF | 8080 | 8080 | `http://localhost:8080` |
| Accounts | 8080 | 8081 | `http://localhost:8081` |
| Expense Core | 8080 | 8082 | `http://localhost:8082` |
| Notifications | 8080 | 8083 | `http://localhost:8083` |

On first creation PostgreSQL runs `init-databases.sql`, which creates
`pennywise_accounts`, `pennywise_expense_core`, and
`pennywise_notifications`. Removing containers with `down` does not request
volume deletion; use explicit Docker volume administration only when a clean
database is intended.

## Native application environment

The checked-in IntelliJ configurations already provide these values. Equivalent
shell launches must set them before the corresponding Gradle `bootRun` task.

| Application | Port | Required local environment |
| --- | ---: | --- |
| Accounts | 8081 | `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/pennywise_accounts`, datasource user/password above |
| Expense Core | 8082 | `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/pennywise_expense_core`, datasource credentials, RabbitMQ host `localhost`, port `5672`, and credentials |
| Notifications | 8083 | `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/pennywise_notifications`, datasource and RabbitMQ values, email host `localhost`, email port `1025` |
| BFF | 8080 | Accounts URL `http://localhost:8081`, Expense Core URL `http://localhost:8082` |

Set `SPRING_PROFILES_ACTIVE=local` and `SERVER_PORT` to the table's port. The
exact variable names are version-controlled in `.run/`.

## Health and startup order

PostgreSQL is healthy only after `pg_isready -U pennywise`; RabbitMQ uses
`rabbitmq-diagnostics -q ping`; Mailpit checks its HTTP UI. Compose waits for
these checks before starting dependent applications. In the full topology,
Accounts waits for PostgreSQL, Expense Core waits for PostgreSQL and RabbitMQ,
Notifications waits for PostgreSQL, RabbitMQ, and Mailpit, and BFF starts after
the three REST services have started.

The dependency targets use `up -d --wait`, so a successful command means the
selected dependency health checks passed. The full stack remains attached in
the foreground so application startup failures are immediately visible; use a
second terminal for `make full-status` or `make acceptance-live`.

## Images and validation

The full and BFF prerequisite topologies use
`infra/docker/Dockerfile.dev` and Gradle `bootRun`. For fast runtime images from
already packaged jars, use `make docker-fast-all`; for production-style
multi-stage images, use `make docker-build-all`. Those image-build commands do
not start a Compose topology.

Validate configuration and public contracts before committing topology changes:

```sh
make compose-config
python3 tools/contracts/validate.py
python3 tools/contracts/validate_public_surface.py
git diff --check
```
