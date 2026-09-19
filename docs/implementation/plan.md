# Implementation plan

## Current deliverable

The reviewed plans, contracts, scaffold, local operations, CI delivery, and
several backend product slices are now present. Product work remains partial and
tracked; UI implementation and public launch acceptance are not complete. All
app features remain free.

## Delivery gates

1. DOC-01 establishes the canonical registry and task specifications.
2. DOC-02/03 architecture, DOC-04/05/06 contracts, and DOC-07 quality/operations
   authoring proceed in parallel on disjoint paths. Contracts and quality may
   draft against the confirmed scope; DOC-08 reconciles all final documents.
3. DOC-08 validates task links/DAG, API schemas/examples, financial fixtures,
   GraphQL operation mapping, and the accepted JPA/Hibernate decision. Record
   documentation_gate.status=passed only with verification evidence.
4. FND-01 creates the build/apps/libs/UI placeholder. FND-02 local infrastructure
   and FND-03 portable checks can proceed in parallel after that shared scaffold.
5. Continue from the verified scaffold into registered product slices and
   hardening. QA-04, OPS-10, and the Production Hardening milestone (OPS-17
   through OPS-23, ERR-01 through ERR-12) are fully implemented and verified.
   CORE-22, QA-05, DOC-20, and OBS-01 have completed their registered increments.
   BFF-06/07/09/10/11 and CORE-23/24/25 have completed their current increments.

## Parallel implementation schedule

Accounts/security and Expense Core can proceed in parallel with messaging/BFF
contract adapters. One owner controls each shared boundary; delegated subagents
work only on registered, non-overlapping child tasks. Outbox integration
and core financial writes require a scheduled handoff rather than concurrent
edits to the same files. Quality work follows each implemented slice; final
end-to-end and capacity testing depend on a fully integrated stack.

## Scope

MVP: profiles, groups, placeholders and claiming, flexible expense splits,
multiple payers, per-currency balances, audited online edits by all active members,
immediate recorded repayments, recurrence, notifications, search/CSV, and offline
read/create synchronization. GraphQL BFF has HTTPS operations and WebSocket
invalidations; internal request/response APIs are REST. Payment processing,
refund workflows, FX conversion, OCR, attachments, offline editing, cross-group
settlement and UI implementation are deferred.

## Toolchain policy

The earlier Java26 proposal was superseded by the verified Java25 baseline;
versions are centralized in the catalog and wrapper. Record dependency-resolution
evidence for future changes and do not introduce preview versions.

## Verification

Documentation checks run before scaffolding. Scaffold checks cover dependency
resolution, compilation, package boundaries, application packaging, contract
validation, and reproducible local configuration. No passing scaffold check is
reported as proof of product behavior or production capacity. All checks and
limitations are recorded in the registry and final handoff.

## Future delivery

The task registry contains the complete implementation DAG and task detail links.
Future UI work will choose a framework separately and consume the GraphQL schema.
Hosting selection, operating budget and launch-market retention policy are public
launch prerequisites; they do not block portable contracts and the scaffold.
# Error reporting hardening plan

The existing `ErrorCode` vocabulary is intentionally treated as a compatibility
baseline, not the final production diagnostic model. The ERR-01 through ERR-12
workstream introduces specific, governed codes that identify service, feature,
operation, and scenario without exposing secrets or unbounded identifiers.

## Target model

Public codes use `<SERVICE>_<AREA>_<SCENARIO>`, for example
`EXP_EXPENSE_INVALID_TOTAL`, `ACC_PROFILE_NOT_FOUND`, and
`BFF_UPSTREAM_EXPENSE_TIMEOUT`. `source`, `component`, and `operation` remain
separate response fields so code names stay stable while implementation detail
can evolve. Every occurrence also receives an `errorId`; `requestId` remains the
request-level correlation key.

The catalog is authoritative for code ownership, default HTTP status,
retryability, severity, safe detail, and deprecation. Codes are never selected
from exception text, database messages, or arbitrary controller strings.
Resource IDs and field names are included only when disclosure is safe; raw
values, tokens, SQL, stack traces, and exception causes remain server-side.

## Delivery sequence

1. ERR-01 defines `contracts/errors/error-catalog.yaml`, naming rules, lifecycle,
   ownership, and a catalog validator.
2. ERR-02 extends `libs/errors` with typed definitions, domain exceptions,
   error IDs, safe metadata, response serialization, and cause-preserving logs.
3. ERR-03 replaces empty Spring Security 401/403 responses with shared problem
   handlers while preserving `WWW-Authenticate`.
4. ERR-04 through ERR-08 migrate Accounts, Expense Core, and Notifications by
   bounded context. Each feature throws its own cataloged error at the invariant
   boundary and tests exact response metadata and side-effect behavior.
5. ERR-09 maps service errors through BFF REST adapters and GraphQL
   `errors[].extensions`, retaining upstream code/source and adding BFF transport
   codes only when the failure is introduced by the gateway.
6. ERR-10 adds negative-path tests across all public interfaces and makes Bruno
   fail on missing code, source, component, operation, requestId, errorId, or
   problem content type.
7. ERR-11 adds bounded Micrometer counters and dashboards. Metric labels may
   include service, component, operation, code, and status, but never requestId,
   userId, groupId, resourceId, exception text, or field values.
8. ERR-12 reconciles contracts, docs, runbooks, release checklists, and local
   Docker evidence. Production-only controls remain explicitly separate.

## Required catalog fields

Each entry must define `code`, `service`, `component`, `operation`, `status`,
`title`, `safe_detail`, `retryable`, `severity`, `owner`, and lifecycle state.
The validator must reject duplicate codes, invalid prefixes, missing owners,
ambiguous status mappings, undeclared public codes, and unsafe metadata.

## Completion gate

The workstream is complete only when no REST or GraphQL endpoint emits an empty
or unstructured failure, every public code has catalog and test evidence, BFF
mapping preserves origin information, security failures are structured, error
metrics are bounded, and `make check`, `make acceptance-live`, Bruno, contract,
and observability validation pass locally.
