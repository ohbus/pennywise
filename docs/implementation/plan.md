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
   hardening. QA-04 and OPS-10 remain release gates; CORE-22, QA-05, DOC-20,
   and OBS-01 are the active follow-up work. BFF-06/07/09/10/11 and CORE-23/24/25
   have completed their current increments.

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
