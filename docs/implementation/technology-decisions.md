# Technology decisions

## Baseline

Use Kotlin on the JVM, Spring Boot, Gradle Kotlin DSL, Spring MVC for stateful
REST services, and WebFlux/Spring GraphQL for the database-free BFF. Version
selection is verified through dependency resolution and centralized in
`gradle/libs.versions.toml`. The current baseline is Java 25, Kotlin 2.4.20,
Spring Boot 4.1.1, and Gradle 9.7.1. The earlier Java 26 proposal was superseded;
future baseline changes require a separately verified technology decision.

## Persistence

Use Spring Data JPA with Hibernate for ordinary aggregate persistence. This
reduces mapping code while retaining transactions, optimistic `@Version`
checks, and fetch planning. Use native SQL only for group-revision locking,
outbox/job claiming and measured specialized queries. Flyway owns migrations;
Hibernate validates. Disable Open Session in View, keep entities private, and
return DTOs. The BFF has no database.

PostgreSQL is authoritative for money, audit, balances, synchronization and
outbox records. Each service has separate credentials and tables. Databases may
share a cluster initially, but cross-service SQL, foreign keys and entities are
forbidden. Store monetary values as integer minor units internally and strings
on JSON/GraphQL boundaries.

## Messaging and API

REST over HTTPS is the synchronous service boundary. RabbitMQ via Spring AMQP
delivers transactional-outbox events to notifications and BFF invalidation
fan-out. Use publisher confirms, durable queues, manual acknowledgements,
deduplicating inbox records, bounded retry and parking queues. A socket is only
an invalidation hint; REST snapshot/change-feed recovery remains authoritative.

GraphQL over HTTPS is the current BFF client API. GraphQL subscriptions over
authenticated WebSockets carry group IDs and revisions, never authoritative
balances. Every BFF replica has its own temporary fan-out queue.

## Security and operations

Production deployments use a provider-neutral OIDC configuration and Spring
Security resource-server validation in every service. Keycloak is permitted as
an optional local OIDC provider for realistic integration testing, but is not an
application dependency and no domain code may depend on Keycloak-specific APIs
or claims. The existing passthrough bearer-token principal remains a narrowly
scoped local-demo mechanism only; it accepts no proof of identity and therefore
does not constitute production or OIDC evidence. See
`docs/security/authentication-hardening.md` and task `AUTH-01` for the staged
hardening plan.
Services enforce membership authorization themselves. Use Actuator,
Micrometer and OpenTelemetry-compatible tracing; structured logs exclude money
payloads, invitation secrets and tokens. Docker Compose supports local
PostgreSQL, RabbitMQ and SMTP capture. Kubernetes, Kafka, Redis, JPA
second-level caching, multi-region writes and sharding are deferred until
measured requirements justify them.
