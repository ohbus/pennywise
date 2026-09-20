# Production validation follow-up

> **Last updated**: 2026-09-20
> **Task**: QA-08
> **Status**: All dimensions open

QA-08 tracks evidence that **cannot be established by the local Docker stack**.
It is deliberately separate from QA-07's executable local public-interface
coverage.

## How to use this document

This document tracks the gap between "works locally" and "works in production."
Every dimension below requires evidence from a **production-like environment**
(Level 4 or Level 5 in the evidence classification — see
[production-readiness-audit.md](../reviews/production-readiness-audit.md#evidence-classification-standard)).

> [!IMPORTANT]
> Local contract, unit, live-integration, and E2E results must **not** be
> promoted to production evidence. QA-08 may close a row only when the
> artifact, environment, threshold, and procedure are recorded.

## Related documents

Use these documents to plan and execute the production validation work:

| Document | Purpose |
|---|---|
| [Production readiness audit](../reviews/production-readiness-audit.md) | Current findings and evidence classification |
| [Production readiness roadmap](../implementation/production-readiness-roadmap.md) | Phase-by-phase execution plan (especially Phase 7) |
| [Production readiness tracker](../tasks/production-readiness-tracker.md) | Workstream ownership and acceptance criteria |
| [Production readiness plan](../operations/production-readiness-plan.md) | Release gate checklist |

## Evidence dimensions

| Dimension | Required evidence | Current status | Target SLO | Roadmap phase |
|---|---|---|---|---|
| **Capacity and latency** | Production-like load profile (60-min soak + burst), p50/p95/p99 artifacts, saturation metrics | 🔴 Open | p50 < 100ms, p95 < 500ms, p99 < 1s, error < 0.1% | Phase 7 (PR-12, PR-16) |
| **Failover and restore** | Database/broker/service recovery rehearsal with data-integrity proof, RPO/RTO measured | 🔴 Open | RPO < 1 min, RTO < 5 min | Phase 7 (PR-16) |
| **Security scanning** | Dependency, image, secret, and API security scans with reviewed exceptions | 🔴 Open | Zero critical/high unresolved | Phase 6 (PR-11) |
| **Rollback** | Deployment rollback rehearsal, post-rollback reconciliation, version-skew evidence | 🔴 Open | Zero data loss on rollback | Phase 7 (PR-14, PR-16) |
| **WebSocket protocol** | Malformed frames, duplicate IDs, backpressure, replay-after-reconnect, timeout/retry semantics | 🟡 Partial — local handshake, auth, reconnect, unsubscribe, and delivery covered by QA-07 | Stable under 100K+ connections | Phase 7 (PR-16) |
| **Secret rotation** | JWT signing key, DB credential, broker credential, OIDC secret rotation without downtime | 🔴 Open | Zero user-visible interruption | Phase 4 (PR-07) |
| **Alert routing** | Each alert condition triggers correct on-call notification within SLA | 🔴 Open | Alert delivery < 5 min | Phase 7 (PR-16) |
| **Multi-replica** | ≥3 replicas per service, cross-replica event fanout, no sticky sessions | 🔴 Open | Traffic distributed, zero errors during scale events | Phase 7 (PR-16) |

## Closure protocol

To close a dimension:

1. Execute the validation in the declared production-like environment
2. Record: exact command, environment details, workload parameters, result
   metrics, and artifacts
3. Record what the evidence does NOT prove (limitations)
4. Have an independent reviewer verify the evidence
5. Update this table with the evidence reference and reviewer
6. Coordinator updates `registry.yaml` and `progress.md`
