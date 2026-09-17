# Current repository state audit (DOC-17)

Date: 2026-09-18

## Outcome

The repository builds and the current four feature increments have coherent task
ownership, but the tracker overstated product and launch completion. ACC-05 and
NOT-09 satisfy their declared slices. CORE-18 and BFF-06 compile and test but
retain material acceptance gaps documented in their task details and follow-up
tasks. DOC-12 remains the sole blocked task; DOC-20 provides a bounded path to a
compatible formatting baseline.

## Git and worktree attribution

The complete commit and path map is in `current-git-task-map.md`. The audited
feature work was separated into ACC-05, CORE-18, BFF-06, and NOT-09 commits.
Commit `e791d7c` belongs to DOC-10 and requires ledger attribution. Historical
commit `0af70ed` changed `.gitignore` without registered ownership and is assigned
to DOC-19 for retrospective documentation.

The branch was subsequently reset and amended into root commit `4ee7a20`, so
pre-squash hashes such as `e791d7c` and `0af70ed` are local-reflog history rather
than commits reachable from current `main`. The Git task map records this
provenance limitation; DOC-19 owns the legacy evidence treatment.

## Drift requiring work

The detailed evidence is in `current-drift-review.md` and
`current-task-state.md`. Registered follow-ups cover missing group lifecycle,
recurrence transport, search/export transport, transactional group-change
effects, complete BFF member resolution, cross-replica invalidations,
contract/status reconciliation, authenticated acceptance, public-launch
evidence, legacy tracker backfill, and formatting baseline work.

## Current classification

- Done: reviewed increments with focused test, contract, and commit evidence.
- In progress: DOC-17, OPS-09, CORE-18, and BFF-06.
- Planned: newly registered corrective/product/quality/operations work.
- Blocked: DOC-12, pending the DOC-20-compatible replacement path.

Product scope remains authoritative, but the repository must not be described as
MVP-accepted or launch-ready until QA-04 and OPS-10 produce their required real
environment evidence.
