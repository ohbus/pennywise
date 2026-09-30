# Clone and run

<p align="left">
  <img src="../visuals/logo.svg" alt="Squarewise Logo" width="300">
</p>

Prerequisites for native development are Java 25, Docker with Compose v2,
and `uv` for isolated Python tooling and virtual environment management.
The wrapper supplies Gradle. No Node runtime is required for the backend
services and their dependency topology.

```sh
make doctor
uv sync --frozen --no-build       # or make sync
make check
```

Choose one local topology. `make deps-up` starts PostgreSQL, RabbitMQ, and
Mailpit for mixed native development. The narrower native-app commands start
only that application's prerequisites:

| Native application | Start prerequisites | Start application |
| --- | --- | --- |
| Accounts | `make accounts-deps-up` | IntelliJ `Squarewise Accounts` or `./gradlew :app:accounts:bootRun` with the documented local environment |
| Expense Core | `make expense-core-deps-up` | IntelliJ `Squarewise Expense Core` |
| Notifications | `make notifications-deps-up` | IntelliJ `Squarewise Notifications` |
| BFF | `make bff-deps-up` | IntelliJ `Squarewise BFF`; the Compose topology supplies Accounts and Expense Core upstreams |

Use the corresponding `-status`, `-logs`, and `-down` targets shown by
`make help`. Do not start multiple topologies simultaneously: they intentionally
publish the same stable host ports.

For the complete containerized environment, run:

```sh
make dev-setup     # Ensures infra/local/.env exists with safe development keys
make compose-up    # Starts Postgres, RabbitMQ, Redis, Keycloak, Mailpit, and 4 services
make seed          # Seeds rich realistic groups, multi-participant expenses, and settlements
```

To seed an extensive high-volume dataset (50+ groups and hundreds of expenses across categories):
```sh
make seed-large
```

To reset and start with a clean slate:
```sh
make seed-reset
```

Stop the stack at any time with:
```sh
make compose-down
```

Application health endpoints are on ports 8080 (BFF), 8081 (Accounts), 8082 (Expense Core), and 8083 (Notifications). Mailpit's web UI is at `http://localhost:8025`. Keycloak IdP is available on port 8090.

The BFF exposes GraphQL HTTP at `POST http://localhost:8080/graphql`; its WebSocket subscription endpoint uses the same `/graphql` path. This route is configured by `spring.graphql.path` and verified by the BFF WebFlux transport tests.

Run `make compose-config` after editing any local Compose file. The full command,
service, port, credential, health-check, and startup-order matrix is in
[Compose topology](compose-topology.md).

The development Compose topology runs host-built service JARs in JRE images.
Run `make package` once after source changes, then `make compose-dev-up`; Docker
does not download Gradle or resolve dependencies during container startup. The
separate JVM image definition remains available for image builds that intentionally
compile inside Docker.

### Bruno API collection

The checked-in `tools/bruno/` collection provides local requests for all four
HTTP services, including the GraphQL BFF and Notifications endpoints. Import
that directory into Bruno and start the stack with `make full-up` before
sending requests. The legacy `local` environment is retained only for isolated
compatibility tests and is not an application deployment profile. For the
Keycloak-backed topology, select
`local-oidc` and inject a real signed token through `SQUAREWISE_BRUNO_TOKEN`;
never place credentials in request files. Override base URLs and tokens
centrally for CI or staging. The collection contains no production
credentials.

The hosted E2E workflow follows the same rule: after starting Compose it uses
the fixture's non-user `squarewise-ci` service account with the OAuth
client-credentials grant to obtain a short-lived signed token. It deliberately
does not use the legacy `test-user` placeholder or require a manually copied
token. The service account is local-CI-only and cannot perform password grants.
The required-services acceptance mode also fails immediately when
`BEARER_TOKEN` is absent; it never silently falls back to the legacy
`test-user` value.

## Live acceptance testing

When the development stack or local services are up, validate the integrated multi-service workflow with:
```sh
make acceptance-live
# or:
python3 tests/acceptance/runner.py --require-services
```
The `--require-services` flag enforces that all live endpoints (Accounts, Expense Core, Notifications, BFF) are reachable and healthy; if any service is offline or a scenario fails, the command exits with code 1. For non-blocking runs when services are offline, `make acceptance` records unavailable services as `blocked` without failing.

For production, build and scan each immutable image with `infra/docker/Dockerfile.jvm`,
run owner migrations, set the four `SQUAREWISE_*_IMAGE` variables, inject secrets
through the deployment platform, and use
`infra/deploy/docker-compose.prod.yml`. Do not commit `.env` files or credentials.

### Production Keycloak Baseline (L-2)

Keycloak in `infra/local/docker-compose.yml` runs in `start-dev` mode strictly for local
developer and CI fixture convenience. In staging and production environments:
- Keycloak must run `kc.sh start --optimized` with build-time optimizations.
- An external hardened PostgreSQL instance must back the Keycloak realm.
- Strict TLS termination (`KC_HOSTNAME_STRICT_HTTPS=true`) and trusted CA certificates must be configured.
- Embedded development features and dev caches must remain disabled.

