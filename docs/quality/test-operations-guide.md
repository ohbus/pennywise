# Test operations guide

This guide explains how the test suites are arranged, what each suite proves,
which services they require, and how to reproduce the CI checks locally. The
authoritative endpoint inventory is the
[`public-interface-operation-matrix.md`](public-interface-operation-matrix.md);
the cross-cutting dimensions and evidence levels are defined in
[`public-interface-coverage.md`](public-interface-coverage.md).

## Test layers

Tests are deliberately layered. A lower layer gives fast, precise feedback; a
higher layer proves that wiring, persistence, security, messaging, and deployed
transport behavior work together. Passing a lower layer is not evidence that a
higher layer passed.

| Layer | Location | Proves | Typical command |
| --- | --- | --- | --- |
| Domain/unit | `app/*/src/test/kotlin/` | Pure validation, allocation, balance, cursor, replay, concurrency, and policy rules | `make test-unit` |
| Controller/transport | `app/*/src/test/kotlin/` | HTTP status/problem mapping, validation, authentication/authorization decisions, DTO mapping | `./gradlew test --tests '*ControllerTest'` |
| Persistence/integration | `app/*/src/test/kotlin/` | JPA, Flyway, PostgreSQL constraints, optimistic locking, outbox and broker interaction | `make test-integration` |
| Contract/static | `contracts/`, `tools/contracts/` | OpenAPI structure, operation IDs, GraphQL roots, examples, REST request inventory | `make contracts` |
| Acceptance | `tests/acceptance/` | Cross-service API scenarios using deterministic test doubles or configured dependencies | `make acceptance` / `make acceptance-live` |
| Live REST edge | `tests/e2e/test_rest_edge_cases.py` | Deployed authentication, authorization, malformed input, cursor boundaries, media negotiation, idempotency and missing resources | `make e2e-rest-edge` |
| Live product E2E | `tests/e2e/test_product_journey.py` | End-to-end group, expense, notification and BFF journeys | `make e2e-live` |
| Live resilience | `tests/e2e/test_offline_resilience.py`, `test_concurrency_subscriptions.py`, `test_chaos_recovery.py` | Sync replay, conflicts, WebSocket invalidation, concurrency, broker outage and outbox recovery | `make e2e-offline`, `make e2e-concurrency`, `make e2e-chaos` |
| Load/operations | `tests/load/`, `tools/ops/` | Representative load, mutation reconciliation, observability and recovery checks | See `make help` and the load section below |

## Repository arrangement

- `app/<service>/src/main` contains implementation and `src/test` contains
  tests owned by that service.
- `libs/` contains shared libraries and their unit tests.
- `contracts/` is the external behavior source of truth. Change a contract
  before changing an externally visible endpoint.
- `tests/acceptance/` contains cross-service acceptance checks and reports.
- `tests/e2e/` contains live deployed checks. Python files are strongly typed
  and must be checked with `uvx`/mypy.
- `tests/bruno/` contains request-level REST coverage. Every contracted REST
  operation must have an assertion-backed request; the semantic validator
  checks this inventory.
- `tests/graphql/` contains GraphQL operation documents and transport checks.
- `tests/load/` contains k6 scripts and load fixtures.
- `tools/contracts/` and `tools/ops/` contain typed validation and operational
  tooling, not application behavior.
- `docs/quality/` records test policy, evidence, limitations, and coverage
  matrices; `docs/tasks/progress.md` records commands and results.

## Recommended local execution order

For a fast implementation loop:

```sh
make contracts
make test-unit
make acceptance
make python-typecheck
```

Before review, run the complete non-live gate:

```sh
make check
make ci
```

`make check` includes formatting/build checks, Gradle tests, contract
validation, acceptance unit tests, workflow validation, and strict Python
typing. `make ci` is the local equivalent of the hosted verification workflow.

For deployed behavior, start the development topology and run the suites in
this order:

```sh
make compose-dev-up
make acceptance-live
make bruno-run
make e2e-rest-edge
make e2e-live
make e2e-offline
make e2e-concurrency
make e2e-chaos
```

Or run the four broad product/resilience suites with `make e2e-all`. The live
stack uses PostgreSQL, RabbitMQ, Mailpit, the three REST services, and the BFF;
the Compose health checks must be green before tests begin. Stop it with
`make compose-dev-down` when finished.

If application code changed after images were built, package the relevant
`bootJar` and rebuild the service image before live testing:

```sh
./gradlew :app:expense-core:bootJar
docker compose -f infra/local/docker-compose.dev.yml build expense-core
docker compose -f infra/local/docker-compose.dev.yml up -d --wait expense-core
```

## What the live suites cover

`test_rest_edge_cases.py` centralizes route templates and checks missing
authentication, non-member resource hiding, non-member expense creation,
malformed UUIDs, invalid allocation and pagination values, malformed and
expired sync cursors, CSV row limits and `Accept` negotiation, idempotent
replay, tampered replay, missing idempotency keys, and missing groups.

The product journey covers the successful cross-service workflow. Offline
resilience covers snapshot/change cursors and replay behavior. Concurrency and
subscription checks cover simultaneous edits, conflict resolution, WebSocket
subscription delivery/filtering and invalidation. Chaos recovery covers broker
outage, retry, transactional outbox recovery and reconciliation.

GraphQL HTTP and WebSocket behavior is covered by BFF controller/unit tests,
GraphQL transport checks, and the live concurrency/subscription suite. The
HTTP transport checks also reject malformed JSON before resolver execution. The
GraphQL schema root inventory and operation-level gaps are tracked in the
public-interface coverage and operation matrices; do not infer complete
per-field edge coverage from a successful schema parse.

## Coverage dimensions

For each public operation, review the applicable dimensions:

1. success response and response shape;
2. request validation and malformed identifiers;
3. missing/invalid authentication;
4. authorization and non-member access;
5. missing, archived, deleted, or expired resource state;
6. pagination and cursor boundaries;
7. idempotency and replay/tampering;
8. optimistic concurrency and duplicate delivery;
9. dependency, timeout, broker, and persistence failures;
10. side effects: postings, audit, sync changes, outbox, notifications, and
    WebSocket invalidation.

Record the highest evidence level actually run: contract, unit/controller,
persistence integration, live integration, or deployed E2E. A test that only
asserts an HTTP status is not sufficient when the operation has financial or
messaging side effects.

## CI and Python tooling

CI runs contract/static validation, formatting and build checks, all Gradle
tests, JaCoCo/report publication, acceptance tests, workflow parsing, and
strict Python typing. The E2E job packages boot jars, builds the Compose images,
runs the live acceptance/Bruno/E2E suites, and always tears the topology down.

Python dependencies must not be installed into the system interpreter. Use
Homebrew `uv`/`uvx` and run:

```sh
make python-typecheck
# equivalent:
uvx --from mypy==1.17.1 mypy --config-file mypy.ini tests tools
```

Every Python function needs parameter and return annotations, concrete
container types, and no untyped public helpers. If a test must call an external
service, type its request/response boundary and make failure output identify
the operation, expected status, actual status, and response body.

## Adding or changing coverage

1. Register or update the task and dependency in `docs/tasks/registry.yaml` and
   `docs/tasks/board.md` as coordinator work.
2. Update the relevant contract and operation-matrix row.
3. Add the lowest-level unit test that exposes the rule or invariant.
4. Add controller/persistence coverage for transport and side effects.
5. Add or update the live E2E assertion when the behavior is externally
   observable. Reuse centralized route constants.
6. Update affected docs and `docs/tasks/progress.md` with exact commands and
   evidence.
7. Run the applicable local and live gates, inspect the diff, and commit one
   coherent increment.

When an edge test fails because implementation behavior is wrong, fix the
implementation first, retain the regression at the underlying layer, and add
the deployed assertion. Never weaken an expected status merely to make a test
pass; update the contract and matrix only when the intended public behavior
has changed.
