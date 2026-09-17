# IntelliJ IDEA development

Shared run configurations live in `.run/` and are committed so a fresh clone
has the same developer entry points. Open the repository as a Gradle project,
start Docker Desktop, and run `Pennywise Local Infrastructure` before starting
an application. The infrastructure configuration starts PostgreSQL, RabbitMQ,
and Mailpit from `infra/local/docker-compose.yml`.

Application configurations use Java 25, the `local` Spring profile, and the
repository root as the working directory. Local credentials are development-only
values from the Compose file; production credentials must never be copied into
these XML files.

The application entries use standard IntelliJ `Application` configurations with
the imported Gradle module and the Kotlin-generated `*ApplicationKt` launcher.
This invokes each real `main(String[])` directly and avoids Spring Boot plugin
start-point ambiguity. Their database, RabbitMQ, SMTP, port, and upstream URL
values mirror the dependency-only Compose topology and host native-run ports.

| Configuration | Gradle project / main class | Purpose |
|---|---|---|
| Pennywise Accounts | `AccountsApplicationKt` / `pennywise.app.accounts.main` | Accounts REST service |
| Pennywise Expense Core | `ExpenseCoreApplicationKt` / `pennywise.app.expense-core.main` | Financial REST service |
| Pennywise Notifications | `NotificationsApplicationKt` / `pennywise.app.notifications.main` | Notification REST service |
| Pennywise BFF | `BffApplicationKt` / `pennywise.app.bff.main` | GraphQL and WebSocket edge |
| Pennywise JVM Tests | Gradle `test` | Complete JVM test suite |
| Pennywise Local Infrastructure | `infra/local/docker-compose.yml` | PostgreSQL, RabbitMQ, Mailpit |

When adding a service, first register its task and update the architecture and
operations documents. Then add its `.run` configuration, main class, port and
environment variables, and validate the repository contracts before committing.

## GraphQL Schema Detection

To allow IntelliJ IDEA and the GraphQL plugin to recognize the GraphQL schema for
Spring GraphQL annotations (`@QueryMapping`, `@MutationMapping`, `@SubscriptionMapping`),
the repository provides root-level `graphql.config.yml` referencing
`contracts/graphql/schema.graphqls`. Additionally, `:app:bff` includes the `contracts/`
directory in `sourceSets.main.resources`, allowing seamless IDE schema resolution
and code completion across GraphQL resolvers without manual configuration.
