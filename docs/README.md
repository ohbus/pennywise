# Documentation index

This index is the navigation entry point for repository documentation. [`AGENTS.md`](../AGENTS.md) is the working-agreement root; the registry and contracts remain authoritative for task state and externally visible behavior.

## Start here

- [Working agreement](working-agreement.md)
- [Product scope](product/mvp.md)
- [Implementation plan](implementation/plan.md)
- [Task registry](tasks/registry.yaml)
- [Task board](tasks/board.md)
- [Progress ledger](tasks/progress.md)
- [Task details](tasks/details/)
- [API implementation status](api/implementation-status.md)

## Architecture

- [Architecture overview](architecture/overview.md)
- [Project structure](architecture/project-structure.md)
- [Error flow](architecture/error-flow.md)
- [Pagination](architecture/pagination.md)
- [CQRS data access](architecture/cqrs-data-access.md)
- [Database and cache access patterns](architecture/database-and-cache-access-patterns.md)
- [Fallback mechanisms](architecture/fallback-mechanisms.md)

## Implementation and contracts

- [Technology decisions](implementation/technology-decisions.md)
- [Implementation plan](implementation/plan.md)
- [CQRS data-access plan](implementation/cqrs-data-access-plan.md)
- [CQRS code inventory](implementation/cqrs-code-inventory.md)
- [Production-readiness roadmap](implementation/production-readiness-roadmap.md)
- [REST contracts](../contracts/rest/)
- [GraphQL contracts](../contracts/graphql/)
- [Event contracts](../contracts/events/)
- [Error contracts](../contracts/errors/)
- [GraphQL operation mapping](../contracts/graphql/operation-mapping.md)

## Quality

- [Testing strategy](quality/testing-strategy.md)
- [Acceptance criteria](quality/acceptance.md)
- [Programming principles](quality/programming-principles.md)
- [Coding guidelines](quality/coding-guidelines.md)
- [Production validation](quality/production-validation.md)
- [Public-interface coverage](quality/public-interface-coverage.md)
- [Public-interface operation matrix](quality/public-interface-operation-matrix.md)
- [Test operations guide](quality/test-operations-guide.md)
- [Qodana audit](quality/qodana-audit.md)

## Operations

- [Operations index](operations/README.md)
- [Quickstart](operations/quickstart.md)
- [CI](operations/ci.md)
- [Compose topology](operations/compose-topology.md)
- [Authentication runbook](operations/authentication-runbook.md)
- [Capacity baseline](operations/capacity-baseline-1m.md)
- [CQRS replica operations](operations/cqrs-replica.md)
- [Error catalog](operations/error-catalog.md)
- [IntelliJ setup](operations/intellij.md)
- [Key rotation runbook](operations/key-rotation-runbook.md)
- [Ledger reconciliation](operations/ledger-reconciliation.md)
- [Local release checklist](operations/local-release-checklist.md)
- [Messaging topology migration](operations/messaging-topology-migration.md)
- [Production hardening](operations/production-hardening.md)
- [Production-readiness plan](operations/production-readiness-plan.md)
- [Provider outage runbook](operations/provider-outage-runbook.md)
- [Release hardening checklist](operations/release-hardening-checklist.md)
- [Session revocation runbook](operations/session-revocation-runbook.md)

## Security

- [Authentication and authorization lifecycle](security/auth-and-authz-lifecycle.md)
- [Authentication audit](security/authentication-audit.md)
- [Authentication hardening](security/authentication-hardening.md)
- [Authentication readiness review](security/authentication-readiness-review.md)
- [Authentication threat model](security/authentication-threat-model.md)
- [Browser session security](security/browser-session-security.md)
- [Cache and session consistency](security/cache-and-session-consistency.md)
- [Endpoint authentication matrix](security/endpoint-authentication-matrix.md)
- [Native client security](security/native-client-security.md)
- [Passwordless API contract](security/passwordless-api-contract.md)
- [Provider grant and revocation boundary](security/provider-grant-revocation-boundary.md)
- [Supply-chain evidence](security/supply-chain-evidence.md)
- [Whole-security implementation audit](security/whole-security-implementation-audit-2026-09-29.md)

## Reviews and visuals

- [Current drift review](reviews/current-drift-review.md)
- [Current Git task map](reviews/current-git-task-map.md)
- [Current state audit](reviews/current-state-audit.md)
- [Current task state](reviews/current-task-state.md)
- [DOC-15A architecture-contract drift](reviews/DOC-15A-architecture-contract-drift.md)
- [DOC-15B build-code drift](reviews/DOC-15B-build-code-drift.md)
- [Production-readiness audit](reviews/production-readiness-audit.md)
- [Visuals index](visuals/README.md)

## Contracts and task detail conventions

The complete task-detail directory is linked above because task scope is selected from [`tasks/registry.yaml`](tasks/registry.yaml), not from this index. Contract files under [`contracts/`](../contracts/) are the source of truth for REST, GraphQL, event, and error behavior.
