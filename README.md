# Pennywise

A permanently free expense-sharing application for households, couples, and
travel groups. The repository contains four Kotlin/Spring applications
(Accounts, Expense Core, Notifications, and the GraphQL BFF), shared contracts,
local Compose topologies, and a documentation-first execution process.

## Delivery status

The backend is partially implemented and continuously verified; this is not yet
an accepted public MVP. See [the task board](docs/tasks/board.md),
[the task registry](docs/tasks/registry.yaml), and
[implementation status](docs/api/implementation-status.md) for the authoritative
state. Planned tasks, directories, or contracts do not imply a completed feature.

## One-command development

Run `make help` for the exhaustive command guide. `make check` validates
contracts, tests, coverage, and application builds. Use `make deps-up` for
infrastructure, `make full-up` for infrastructure plus all applications,
`make compose-config` to inspect resolved Compose files, and `make acceptance`
for the current public-interface harness. Standalone service Compose files and
IntelliJ launchers are documented in `docs/operations/`.

## Accepted direction

Kotlin and Spring Boot, Gradle Kotlin DSL, REST domain services, a GraphQL BFF,
PostgreSQL, Spring Data JPA/Hibernate with targeted SQL, and RabbitMQ with a
transactional outbox. The future UI has a reserved `app/web/` workspace.

## How work is conducted

Fresh sessions follow the tracker and evidence workflow in
[docs/working-agreement.md](docs/working-agreement.md). Mandatory design rules
are in [docs/quality/programming-principles.md](docs/quality/programming-principles.md);
every implementation increment has a registered task, scoped files, validation
evidence, and a small descriptive commit.
