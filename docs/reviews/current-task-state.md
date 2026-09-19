# Current task-state audit (DOC-17A)

> Historical snapshot from 2026-09-18. The live registry and board are
> authoritative for current task status.

Snapshot: 2026-09-18, working tree based on `e791d7c`. This report compares
`docs/tasks/registry.yaml`, `docs/tasks/board.md`, task details, the progress
ledger, dependency state, and committed history. It does not treat uncommitted
implementation or an agent report as accepted evidence.

## Current state

- The registry contains 79 tasks: 74 `done`, 4 `in_progress`, and 1 `blocked`.
  The board contains the same 79 IDs, but reports 70 `done`, 8 `in_progress`,
  and 1 `blocked`.
- The only registered blocked task is DOC-12. Its blocker is explicit: the
  attempted ktlint combinations are incompatible with Kotlin 2.4.20. DOC-14
  subsequently established that Spotless/ktfmt resolves but found 65 existing
  violations; no follow-up task is registered to remediate those violations and
  close or supersede DOC-12.
- DOC-17 and its three audit children are the intended active work. All
  specifications referenced by the registry exist, and every dependency ID
  resolves.

## Inconsistencies requiring coordinator action

1. **Registry and board disagree on four product tasks.** The registry marks
   CORE-18, BFF-06, NOT-09, and ACC-05 `done`; the board marks each
   `in_progress`. Their implementation, contracts, task details, registry
   entries, and board entries are all uncommitted. The progress ledger contains
   no row for any of the four. Therefore none currently satisfies the repository
   completion protocol (reviewed deliverable, recorded exact verification,
   coherent implementation commit, and ledger commit hash). Keep them
   `in_progress` until review and commit, or commit the reviewed implementation
   and update board, registry, and ledger together.

2. **DOC-17A/B/C have an unsatisfied dependency while already in progress.**
   Each child depends on DOC-17, which is itself `in_progress` and cannot finish
   until the child reports are complete. This contradicts the rule that
   dependencies be complete before a task starts and makes the audit hierarchy
   dependency-inverted. Make DOC-17 their parent only and set each child's
   dependencies to DOC-15 and DOC-16 (or no additional dependency if the parent
   is used only for grouping). DOC-17 should consume their results before it is
   marked done.

3. **The DOC-17 task detail and registry disagree on ownership.** The detail
   claims `docs/tasks/details/DOC-17*.md`; the registry gives DOC-17 only
   `docs/tasks/details/DOC-17.md`, while each child owns its own detail. Retain
   the non-overlapping registry model and correct the DOC-17 detail. The
   DOC-17A/B/C details are also only one-sentence briefs and omit the dependency,
   owned-path, validation, and expected-evidence sections present in the
   registry; expand them before final acceptance.

4. **The latest DOC-10 correction is not represented in tracker evidence.**
   Commit `e791d7c` repaired the IntelliJ configuration type and local service
   settings and updated `docs/tasks/details/DOC-10.md`, but DOC-10's registry
   evidence still names only `d504d13`, and the progress ledger has no DOC-10
   correction row for `e791d7c`. Add the executed validation and correction
   commit to the ledger and registry evidence; do not change DOC-10's `done`
   state.

5. **Legacy `done` records do not meet the current evidence schema.** DOC-02
   through DOC-08 and DOC-15A/B have empty registry evidence. DOC-01 through
   DOC-08, FND-01 through FND-03, ACC-01, CORE-01 through CORE-06, MSG-01,
   NOT-01, BFF-01/BFF-02, QA-01, and OPS-01/OPS-02 have no registry validation
   commands. The progress ledger supplies partial historical evidence for many
   of them, but not the exact exit status, report path, version, and limitation
   required by the ledger's current evidence rule. Backfill evidence from Git
   history where it is recoverable; otherwise label the unavailable fields as
   historical limitations rather than inventing results.

6. **Two completed tasks have no assigned owner agent.** DOC-08 and CORE-03
   have `owner_agent: null`, contrary to the one-owner rule. Record the actual
   historical owner (likely the coordinator, subject to Git/history review).

7. **Three registered task specifications are not individual task details.**
   DOC-11 points its `specification` at `docs/architecture/pagination.md`, and
   DOC-15A/B both point at `docs/tasks/details/DOC-15.md`. The paths exist, so
   the current link check passes, but this does not provide the per-task detail
   required by the working agreement. Add DOC-11, DOC-15A, and DOC-15B detail
   files or explicitly document and validate the accepted shared-spec exception.

8. **Documentation-gate counts are historical but read as current.** The gate
   evidence says "25 tasks registered" while the live registry contains 79.
   Preserve the original gate fact with an explicit "at gate passage" qualifier,
   rather than replacing it with a current count.

## Recommended update sequence

1. Review and commit CORE-18, BFF-06, NOT-09, and ACC-05 as separate meaningful
   increments (or revert their status to `in_progress` if review fails), then add
   exact ledger rows and synchronize board and registry.
2. Correct the DOC-17 child dependencies and DOC-17 detail ownership before
   accepting the audit reports.
3. Add the `e791d7c` DOC-10 correction evidence.
4. Register a bounded lint-remediation/follow-up task based on DOC-14, then
   decide whether it supersedes or unblocks DOC-12.
5. Backfill the legacy ownership, task-detail, validation, and evidence gaps in
   a documentation-only task so product implementation commits remain small.

No dependency IDs or specification paths are missing, and the board has neither
extra nor missing task IDs. Those structural checks pass; the state/evidence
issues above prevent DOC-17's reconciliation criterion from passing yet.
