# Target project structure

This is the target after the documentation gate; describing it does not authorize
application creation before DOC-08 passes.

```text
pennywise/
├── AGENTS.md
├── README.md
├── settings.gradle.kts
├── build.gradle.kts
├── gradlew / gradlew.bat
├── gradle/                 # wrapper and libs.versions.toml
├── build-logic/            # convention plugins, no business logic
├── app/
│   ├── accounts/
│   ├── expense-core/
│   ├── notifications/
│   ├── bff/
│   └── web/README.md       # reserved; no framework or Gradle participation
├── libs/
│   ├── db/                # database/JPA technical configuration
│   ├── security/          # token validation and identity plumbing
│   ├── observability/     # telemetry conventions
│   └── test-support/      # reusable fixtures/containers, no production dependency
├── contracts/
│   ├── rest/              # accounts, expense-core, notifications OpenAPI
│   ├── graphql/           # BFF schema and operations
│   ├── events/            # envelopes and event payload schemas
│   └── examples/          # valid/invalid and financial fixtures
├── tools/contracts/       # portable validation
├── tests/                 # cross-service acceptance and performance
├── infra/
│   ├── local/             # Docker Compose and development configuration
│   └── deploy/            # portable deployment reference
└── docs/                  # product, architecture, tasks, quality, operations
```

Every backend application owns its source, tests, resources, migrations where
applicable, configuration and packaging. `libs/db` does not export entities,
repositories or Flyway migrations. BFF must not depend on `libs/db`. Test support
is consumed only from test configurations. Generated clients/types belong under
build output and derive from approved contracts; do not hand-maintain a shared
business-model library.

Persistence and service implementations must adhere to enterprise file separation:
persistent entities (`@Entity`), Spring Data repository interfaces (`@Repository`),
and service/adapter classes (`@Service`) must reside in individual source files,
never bundled together in a single file.

Application source packages follow feature slices, as demonstrated by
`app/expense-core`: controllers, domain models, persistence ports, repositories,
and adapters for one capability stay under that capability's package. Other
applications and libraries should use the same convention; cross-cutting
configuration belongs in an explicit `security`, `messaging`, or equivalent
technical package rather than at the application package root. The Accounts
security configuration and OIDC subject validator now follow this convention
under `accounts.security`.

Shared ID primitives are likewise grouped by concern under `libs/ids`:
endpoint contracts, event constants, and UUID generation. Their existing public
package remains stable so application imports do not need a breaking migration.

Shared error handling follows the same concern grouping under `libs/errors`:
domain error definitions, HTTP problem mapping, and request-correlation
plumbing are separated physically while retaining the established public
package for consumers.

The BFF keeps GraphQL, REST transport, realtime fanout, and messaging concerns
in separate source folders so transport adapters do not sit beside application
startup code.

Future `app/web` chooses tooling in a separate UI task. Its README explains the
GraphQL endpoint, authentication integration decision still required, subscription
and reconnect behavior, money-as-string representation, and pending offline
states. The initial deliverable contains no UI runtime or package manifest.

> Verification: Notifications module audited, KDoc and logger placeholders verified, and test suite passed.
