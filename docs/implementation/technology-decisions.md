# Technology decisions

## Baseline

Use Kotlin on the JVM, Spring Boot, Gradle Kotlin DSL, Spring MVC for stateful
REST services, and WebFlux/Spring GraphQL for the database-free BFF. Version
selection is performed by FND-01 from stable artifact metadata; Java 25 is
currently installed locally, while Java 26 remains the candidate target.

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

Use an OIDC provider and Spring Security resource-server JWT validation in every
service. Services enforce membership authorization themselves. Use Actuator,
Micrometer and OpenTelemetry-compatible tracing; structured logs exclude money
payloads, invitation secrets and tokens. Docker Compose supports local
PostgreSQL, RabbitMQ, OIDC and SMTP capture. Kubernetes, Kafka, Redis, JPA
second-level caching, multi-region writes and sharding are deferred until
measured requirements justify them.
