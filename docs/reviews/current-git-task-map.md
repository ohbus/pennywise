# Current Git and worktree task map

Audit task: DOC-17B

Snapshot: `e791d7c` (`2026-09-18`), with the worktree inspected on
`2026-09-18`

History window: commits after the completed DOC-15 audit (`13413b9`) through
`HEAD`

## Summary

All 29 paths present in the initial worktree snapshot map to registered tasks. The 20
application and contract paths belong to the four completed-but-uncommitted
increments ACC-05, CORE-18, BFF-06, and NOT-09. The two shared tracker paths and
seven new task-detail paths belong to coordinator task DOC-17 or the task they
specify. No application path is unmapped.

During validation, `.kotlin/sessions/kotlin-compiler-7340875304666408377.salive`
appeared as an additional untracked compiler-session artifact, presumably from a
concurrent Gradle run. It does not belong to a task or deliverable and must not be
committed. `.kotlin/` is not currently ignored. The coordinator should remove the
stale generated artifact once active Gradle processes finish and register any
`.gitignore` policy change before editing that shared repository-control path.

The history window contains 40 commits. Feature commits generally have a clear
task association, but several coordinator/integration subjects omit task IDs.
Two history defects need coordinator action:

1. `e791d7c` is a corrective DOC-10 increment, but its subject omits DOC-10 and
   `docs/tasks/progress.md` contains only the original DOC-10 evidence at
   `d504d13`, not the repair.
2. `0af70ed` changes `.gitignore` to ignore agent workspace symlinks, but no
   registered task owns `.gitignore`; this commit is not traceable to a task.

The current worktree also has an execution-state mismatch: ACC-05, CORE-18,
BFF-06, and NOT-09 are `done` in the registry and their details claim successful
verification, while their implementation remains uncommitted, the board still
shows all four as `in_progress`, and no progress-ledger entries record commits.
Their files should stay grouped into four small task-specific commits, followed
by a separate coordinator tracker/evidence commit.

## Complete worktree mapping

This table covers every path returned by `git status --short` at the snapshot.

| Task | Status / paths | Why the path belongs to the task |
| --- | --- | --- |
| ACC-05 | Modified: `app/accounts/src/main/kotlin/com/subhrodip/pennywise/accounts/JpaProfileStore.kt`, `app/accounts/src/main/kotlin/com/subhrodip/pennywise/accounts/ProfileController.kt`, `app/accounts/src/test/kotlin/com/subhrodip/pennywise/accounts/JpaRequestStoresTest.kt`, `app/accounts/src/test/kotlin/com/subhrodip/pennywise/accounts/ProfileControllerTest.kt`, `contracts/rest/accounts.openapi.json`; untracked: `docs/tasks/details/ACC-05.md` | Batch profile lookup store/controller behavior, tests, REST contract, and task specification. All are explicitly owned by ACC-05. |
| CORE-18 | Modified: `app/expense-core/src/main/kotlin/com/subhrodip/pennywise/expensecore/groups/GroupController.kt`, `app/expense-core/src/main/kotlin/com/subhrodip/pennywise/expensecore/groups/JpaGroupStore.kt`, `app/expense-core/src/test/kotlin/com/subhrodip/pennywise/expensecore/groups/GroupControllerTest.kt`, `app/expense-core/src/test/kotlin/com/subhrodip/pennywise/expensecore/groups/JpaGroupStoreTest.kt`, `contracts/rest/expense-core.openapi.json`; untracked: `docs/tasks/details/CORE-18.md` | Group rename endpoint, persistence, tests, OpenAPI declaration, and task specification. All are explicitly owned by CORE-18. |
| BFF-06 | Modified: `app/bff/src/main/kotlin/com/subhrodip/pennywise/bff/GroupGraphqlController.kt`, `app/bff/src/main/kotlin/com/subhrodip/pennywise/bff/RestGateway.kt`, `app/bff/src/test/kotlin/com/subhrodip/pennywise/bff/GroupGraphqlControllerTest.kt`, `contracts/graphql/schema.graphqls`; untracked: `docs/tasks/details/BFF-06.md` | Group update/member GraphQL aggregation, gateway behavior, tests, schema, and task specification. All are explicitly owned by BFF-06. |
| NOT-09 | Modified: `app/notifications/src/main/kotlin/com/subhrodip/pennywise/notifications/InboxController.kt`, `app/notifications/src/main/kotlin/com/subhrodip/pennywise/notifications/JpaNotificationInboxStore.kt`, `app/notifications/src/test/kotlin/com/subhrodip/pennywise/notifications/InboxControllerTest.kt`, `app/notifications/src/test/kotlin/com/subhrodip/pennywise/notifications/JpaNotificationInboxStoreTest.kt`, `contracts/rest/notifications.openapi.json`; untracked: `docs/tasks/details/NOT-09.md` | Mark-as-read endpoint, persistence, tests, OpenAPI declaration, and task specification. All are explicitly owned by NOT-09. |
| DOC-17 | Modified: `docs/tasks/registry.yaml`, `docs/tasks/board.md`; untracked: `docs/tasks/details/DOC-17.md` | Coordinator-owned registration, board queue, and parent audit specification. |
| DOC-17A | Untracked: `docs/tasks/details/DOC-17A.md` | Registered child audit specification. |
| DOC-17B | Untracked: `docs/tasks/details/DOC-17B.md` | Registered Git/worktree audit specification. This report is also owned by DOC-17B but did not exist at the initial snapshot. |
| DOC-17C | Untracked: `docs/tasks/details/DOC-17C.md` | Registered drift-review specification. |
| Generated / no task | Untracked after the initial snapshot: `.kotlin/sessions/kotlin-compiler-7340875304666408377.salive` | Kotlin compiler-session output created during concurrent validation. This is not source or evidence and must be excluded from commits. |

Although older broad tasks own some parent directories, the new leaf tasks are
the operative owners because their registered scopes and objectives exactly
match these diffs. Shared trackers remain coordinator-only DOC-17 paths.

## Commit-to-task map since DOC-15

Documentation/evidence commits are paired with the implementation they close
where that makes the relationship clearer.

| Commit(s) | Task mapping | Files / purpose and assessment |
| --- | --- | --- |
| `4c5857b` | DOC-15B | Added the code/build drift report. Clear from path and subject. |
| `a25dd60` | DOC-15 | Reconciled audit findings in reusable CI and the shared trackers. The generic subject does not name DOC-15, but the task detail and ledger do. |
| `283aeb3`, `698acf0` | DOC-16 | Reconciled REST/GraphQL implementation status, then closed DOC-16 in trackers/evidence. Clear relationship. |
| `de11ffe`, `b4770f7` | CORE-08 | Settlement persistence implementation, then a combined integration/tracker commit. |
| `7cd8ebb`, `b4770f7` | NOT-02 | Preference persistence implementation, then the same combined integration/tracker commit. `b4770f7` is intentionally multi-task but its subject names neither task. |
| `e62dab8`, `8659ffc` | CORE-09 | Durable groups/invitations, then completion tracking. |
| `b14ab0b`, `8659ffc` | NOT-03 | Durable inbox/deduplication, then completion tracking. |
| `f9ecede`, `d30cb7e` | NOT-04 | Transactional notification consumption and completion tracking. |
| `3b61803`, `d30cb7e` | CORE-10 | Durable outbox and completion tracking. |
| `e690fd0` | CORE-11, NOT-05 (plus shared dependency-catalog preparation) | Registered both tasks and added AMQP dependency metadata. Multi-task preparatory commit; subject omits IDs. |
| `6efce73`, `57c7c27` | NOT-05 | Rabbit listener/ack adapter and its progress-ledger entry. |
| `f851403` | CORE-12, CORE-13, ACC-02 (coordinator registration) | Registered the next tasks and reconciled contract metadata. Multi-task coordinator commit; subject names only part of its purpose. |
| `b8edae9`, `865f5cf` | CORE-11 | Durable synchronization persistence and its ledger entry. |
| `42166ea`, `c5f4dbb` | CORE-12 | Scheduled outbox relay and its ledger entry. |
| `456197f`, `8afed60` | FND-02, OPS-03 | Service-isolated databases/runtime fixes and recorded container verification. The progress ledger explicitly attributes both tasks. |
| `ea5d287`, `6980339` | CORE-13 | Durable expense/ledger persistence and its ledger entry. |
| `19d44fe`, `5b68695` | ACC-02 | Durable account deletion/export persistence and its ledger entry. The implementation commit also edits shared trackers/progress, so the following evidence-only commit is partly redundant but still attributable. |
| `0af70ed` | **Unmapped** | Added agent-workspace ignores to `.gitignore`. No task owns `.gitignore`, and the subject has no task ID. Register/attribute this repository-maintenance change retrospectively or document it under an existing coordinator task. |
| `7cb2b52`, `5895941` | CORE-14 | Expense update/delete/reversal implementation and verification ledger entry. |
| `49a249e`, `7fd8f2f` | BFF-03 | GraphQL resolver implementation and verification ledger entry. The implementation also modified Expense Core/controller contracts as supporting BFF work; those paths were recorded under this registered task in that increment. |
| `f05a10d` | CORE-15, QA-03, NOT-06 (coordinator registration) | Registered three parallel tasks. Clear IDs in subject. |
| `57ded4a`, `a362607` | CORE-15, QA-03, NOT-06, ACC-03 | Combined four independent implementations, followed by a combined progress entry. Correctly attributable, but not a small/coherent per-task commit. |
| `222149c`, `af3efb3` | CORE-16, BFF-04, NOT-07, OPS-08 | Combined four independent implementations, followed by a combined progress entry. Correctly attributable, but not a small/coherent per-task commit. |
| `673ea07`, `a5de58a` | CORE-17, BFF-05, NOT-08, ACC-04 | Combined four independent implementations, followed by a combined progress entry. Correctly attributable, but not a small/coherent per-task commit. |
| `e791d7c` | DOC-10 corrective increment | Repaired four IntelliJ application configurations and updated IntelliJ operations/task-detail documentation. The paths and detail make DOC-10 unambiguous, but the commit subject omits the ID and no new progress-ledger evidence records this repair. |

## Ownership and history drift requiring action

- Commit the current feature work by task: one commit each for ACC-05,
  CORE-18, BFF-06, and NOT-09. Do not reproduce the recent four-task aggregate
  commit pattern.
- Only after those hashes exist, reconcile the coordinator-owned registry,
  board, and progress ledger in a separate DOC-17 commit. A task should not be
  `done` before its coherent increment and exact verification evidence are
  recorded.
- Add the DOC-10 repair (`e791d7c`) to `docs/tasks/progress.md`, including the
  actual XML/contract/diff validation already documented by the repair.
- Resolve the unowned `.gitignore` commit (`0af70ed`) by explicit retrospective
  attribution or a registered repository-maintenance task. Do not silently
  infer ownership from a broad scaffold task.
- Prefer task IDs in every implementation, corrective, and evidence commit
  subject. Generic subjects such as `b4770f7`, `e690fd0`, and `f851403` are
  reconstructable from their files but impose avoidable audit work.

## Post-audit history rewrite

After the snapshot above, the shared branch was reset to `d7c4f2c` and amended
as new root commit `4ee7a20` at 2026-09-18 12:14 local time. Consequently, the
40-commit history mapped above and the first DOC-17A/B/C commits are no longer
reachable from `main`; their hashes remain visible only through the local
reflog. The files and cumulative implementation are present in `4ee7a20`, but
the progress ledger's older commit hashes must now be treated as historical
pre-squash references rather than reachable provenance.

The reachable post-squash history begins at `4ee7a20` and then contains the
small task-specific feature, Compose, run-configuration, audit, and evidence
commits produced by this reconciliation. DOC-19 should decide whether to retain
the pre-squash hashes with this limitation or replace them with path-level
evidence from the squashed baseline; it must not invent one-to-one replacement
hashes that no longer exist.
