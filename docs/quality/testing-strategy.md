# Testing strategy

Use the approved toolchain evidence in the architecture documents; earlier
proposed version numbers are not dependencies until resolved successfully.

| Layer | Tooling and purpose |
| --- | --- |
| Domain | JUnit Jupiter and AssertJ; deterministic allocation, balances, schedule policy and authorization decisions |
| Properties | Kotest property library under JUnit; allocation invariants with reproducible seeds |
| Persistence | Testcontainers PostgreSQL; real constraints, JPA versioning, locks, Flyway and native SQL interaction |
| Messaging | Testcontainers RabbitMQ; confirms, retries, deduplication, worker crashes and broker outages |
| HTTP/GraphQL | Spring test clients and GraphQlTester; status/error mapping, real authorization and contracts |
| Architecture | ArchUnit; no domain dependency on transport, no cross-service persistence imports, module APIs only |
| Contracts | OpenAPI validation and breaking-change diff; GraphQL validation/diff; JSON Schema event/example checks |
| Capacity | k6 or equivalent pinned runner; representative HTTP and subscription workloads |
| Supply chain | Dependency inventory/SBOM, vulnerability and secret scanning with reviewed findings |

Every JVM subproject applies JaCoCo and emits XML and HTML reports. Product
milestones must add a meaningful threshold for their changed modules; the empty
scaffold does not impose a misleading global percentage because most behavior
does not exist yet.

Scaffold checks only need meaningful boot/context, packaging, module and contract
verification. Do not create placeholder business tests that merely repeat empty
implementations. Introduce business suites with their implementing task.

The first scaffold quality slice contains deterministic allocation tests and a
Spring application-context smoke test. `tests/e2e/contract-smoke.sh` validates
the public contract and every Compose configuration. It is an infrastructure
smoke test, not a claim that product journeys work; QA-01 adds API-driven E2E
journeys after those endpoints exist.

Contract tooling may be Node-based without making the backend or reserved UI a
Node application. Pin versions and lock dependencies when introducing those tools.
Keep portable entry points runnable without a specific CI provider.

## Persistence-specific checks

Flyway owns schema evolution; Hibernate runs validation, not schema mutation.
Disable Open Session in View. API responses use DTOs rather than entities. Exercise
lazy relationship access inside declared transaction boundaries and detect N+1
queries on representative list endpoints through measured SQL counts.

Native update/claim queries must explicitly account for pending JPA flushes and
stale persistence-context state. Verify version checks cannot be bypassed by a
native mutation. Simultaneous workers use independent transactions/connections;
tests enclosed in one automatically rolled-back transaction cannot prove these
behaviors. Bound timeouts so deadlocks fail promptly with useful traces.

## CI ordering

Run fast schema/style/unit/architecture checks before database and broker suites.
Package each app, scan artifacts, then run integrated acceptance for product
changes. Load and destructive recovery suites run in isolated, explicitly selected
environments. Never execute them against a developer's reused personal database
or production. Retain failing seeds and logs with sensitive values redacted.

The scaffold phase must document the exact supported local commands after they
exist. This file intentionally does not claim a command or test suite already
exists. Hosting-specific tests and browser tests are later milestone work.
