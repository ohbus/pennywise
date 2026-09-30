# Test-coverage change audit

**Scope:** every branch commit from merge-base `65b2fb66d61d90b4d92e4e23a55e06f43cd89270` through
`dfa25ae`, whose subject indicates tests, coverage, QA, JaCoCo, or E2E work.

## Rule applied

Coverage improvements must come from executable unit, integration, messaging, or
E2E tests and their acceptance evidence. Existing implementation logic and
public contracts are preserved. A behavior change is acceptable only when an
observed defect or an explicit requirement justifies it, with a focused test,
documentation, and a separately reviewable commit.

## Audit result

The audit found no contract-file changes in the test/coverage/QA commit set.
Most commits were test, tooling, or documentation-only. Four commits had
production implementation edits:

| Commit | Finding | Disposition |
| --- | --- | --- |
| `aeb31e5` | Removed explicit email domain/label validation and simplified the JWT fallback terminal state while closing branch gaps. | Restored. Tests remain; JaCoCo must measure the implementation that ships. |
| `19d7a1b` | Added blank-message fallbacks after tests exposed unsafe response detail behavior, but also removed the explicit `ErrorCode` HTTP-status fallback mapping. | Blank-message behavior retained as a tested defect correction; status fallback restored. |
| `a52b467` | Changed the login abuse window boundary from exclusive to inclusive at exact expiry. | Retained as an evidenced behavior correction: the acceptance invariant is that a request at expiry is allowed. |
| `fc42e27` | Removed an unused private profile problem mapper in a test-coverage commit. | Restored. Any dead-code cleanup requires a separate justification and review. |

The following test/coverage/QA commits were implementation-preserving or
documentation/tooling-only: `13a441c`, `1d7f028`, `664d504`, `f19d3da`,
`a6812eb`, `0ed2e7d`, `5275496`, `3ca0573`, `d4e28fd`, `d1beb19`,
`2118925`, `ecc0ba5`, `c9464c6`, `ea56495`, `176427c`, `9983199`,
`89b3371`, and `dfa25ae`.

## Current evidence

After restoring implementation logic and retaining the added tests, the current
reports contain 217 methods with missed branches. The remaining count is an
honest discovery baseline, not a claim that any implementation was removed to
improve metrics. The affected wrapper suites pass under Java 25; the full
repository gate remains open until all modules and environment-owned evidence
are rerun.
