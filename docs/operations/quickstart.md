# Clone and run

Prerequisites for native development are Java 25 and Docker with Compose v2.
The wrapper supplies Gradle. Contract validation needs Python 3.11+; no Node
runtime is required for the backend services and their dependency topology.

```sh
make doctor
make check
```

Choose one local topology. `make deps-up` starts PostgreSQL, RabbitMQ, and
Mailpit for mixed native development. The narrower native-app commands start
only that application's prerequisites:

| Native application | Start prerequisites | Start application |
| --- | --- | --- |
| Accounts | `make accounts-deps-up` | IntelliJ `Pennywise Accounts` or `./gradlew :app:accounts:bootRun` with the documented local environment |
| Expense Core | `make expense-core-deps-up` | IntelliJ `Pennywise Expense Core` |
| Notifications | `make notifications-deps-up` | IntelliJ `Pennywise Notifications` |
| BFF | `make bff-deps-up` | IntelliJ `Pennywise BFF`; the Compose topology supplies Accounts and Expense Core upstreams |

Use the corresponding `-status`, `-logs`, and `-down` targets shown by
`make help`. Do not start multiple topologies simultaneously: they intentionally
publish the same stable host ports.

For the complete containerized environment, run:

```sh
make full-up
```

This builds and runs PostgreSQL 17, RabbitMQ 4.3, Mailpit, Accounts, Expense
Core, Notifications, and BFF from `infra/local/docker-compose.dev.yml`. Stop it
with `make full-down`; `compose-dev-up`, `compose-dev-down`, `compose-up`, and
`compose-down` remain compatibility aliases. Application health endpoints are
on ports 8080 (BFF), 8081 (Accounts), 8082 (Expense Core), and 8083
(Notifications). Mailpit's web UI is at `http://localhost:8025`.

The BFF exposes GraphQL HTTP at `POST http://localhost:8080/graphql`; its
WebSocket subscription endpoint uses the same `/graphql` path. This route is
configured by `spring.graphql.path` and verified by the BFF WebFlux transport
tests.

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
sending requests. The legacy `local` environment is only for the explicit
passthrough compatibility profile. For the Keycloak-backed topology, select
`local-oidc` and inject a real signed token through `PENNYWISE_BRUNO_TOKEN`;
never place credentials in request files. Override base URLs and tokens
centrally for CI or staging. The collection contains no production
credentials.

## Live acceptance testing

When the development stack or local services are up, validate the integrated multi-service workflow with:
```sh
make acceptance-live
# or:
python3 tests/acceptance/runner.py --require-services
```
The `--require-services` flag enforces that all live endpoints (Accounts, Expense Core, Notifications, BFF) are reachable and healthy; if any service is offline or a scenario fails, the command exits with code 1. For non-blocking runs when services are offline, `make acceptance` records unavailable services as `blocked` without failing.

For production, build and scan each immutable image with `infra/docker/Dockerfile.jvm`,
run owner migrations, set the four `PENNYWISE_*_IMAGE` variables, inject secrets
through the deployment platform, and use
`infra/deploy/docker-compose.prod.yml`. Do not commit `.env` files or credentials.
