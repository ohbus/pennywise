# MVP product and future experience

## Product promise

Help roommates, couples and travel groups record shared spending, understand every
balance, and record settling up without ambiguity. All features remain free.
There is no guaranteed success formula: validate usefulness, retention and cost
with real groups before widening launch. This phase delivers contracts and a
partially implemented backend slices with UI screens still deferred; this is
not evidence of a launch-ready product.

## Capability and ownership matrix

| Capability | MVP behavior | Owner | Future UI acceptance |
|---|---|---|---|
| Account | OIDC sign-in, profile, preferred currency/timezone | Accounts | Minimal sign-up, return to invitation after authentication |
| Group | Create, rename, archive; household/couple/trip labels | Expense Core | One common workflow with appropriate defaults |
| Participants | Registered members, named placeholders, invitations and claiming | Expense Core | Record expenses before everyone signs up; show invite state |
| Expense | Description, date, category, note, currency, multiple payers | Expense Core | Amount first, current user payer default, visible saved/pending state |
| Splits | Equal, exact, integer percentage basis points, positive weighted shares | Expense Core | Preview every allocation and rounding before saving |
| Collaboration | Every active member edits online; version conflicts and audit | Expense Core | Identify editor; offer reload on conflict instead of silent overwrite |
| Balances | Net amounts by group, participant and currency | Expense Core | Explain each amount through supporting transactions |
| Settle up | Group/currency suggestions; immediate recorded repayment and audited reversal | Expense Core | Say payment was recorded, never imply money transferred |
| Recurrence | Weekly/monthly, start/end, timezone, pause/resume | Expense Core | Clear next occurrence and pause controls |
| Offline | Cached authorized reads and queued creation only | Expense Core; future client queue | Keep pending entries separate from confirmed totals |
| Search/export | Group filters and CSV | Expense Core | Find records and independently inspect spending |
| Notifications | Inbox, email, delivery preferences, opt-in reminders | Notifications | Useful deduplicated notifications, no unsolicited reminder floods |
| Client aggregation | GraphQL operations and WebSocket change hints | BFF | Fast coherent screens with explicit partial/error states |

Group administrators manage membership. Removing a participant never destroys
historical records or silently clears their balance. Claiming binds an account to
an existing participant identifier; it does not rewrite ledger entries. Invitations
expire, can be revoked, and cannot be claimed twice by competing accounts.

## Primary journeys

1. Create a group, add placeholders, enter a first equal-split expense, then invite
   others. Invitees can review context before claiming the correct participant.
2. Open a group, see currency-separated balances and recent activity, add an
   expense, preview its split, and receive a clear confirmation.
3. Edit an expense online; concurrent modification returns the current version
   and a conflict prompt. An activity entry identifies the editor.
4. Reconnect after offline creation. Pending entries upload once, rejected entries
   retain editable local information, and fresh confirmed balances replace cache.
5. Review settlement suggestions, record an external payment, and see an audit
   entry and immediate balance update. Either involved active registered member
   can reverse the recording with a reason.

The eventual UI must support keyboard interaction, screen-reader labels, visible
focus, non-color-only state, accessible forms, useful empty/error states and
locale-aware formatting. Aim for ordinary equal-split entry in about 15 seconds
in usability testing; this is a validation target, not a measured result.

## Success measurement

Track activated groups (two registered participants and three expenses in seven
days), multi-member contribution, four-week household reuse, trip completion and
subsequent group creation. Track duplicate/sync/conflict incidents and time to
resolve balance questions. Report cost per active group alongside retention:
free access does not remove hosting, email and support costs. Do not collect
expense descriptions or other financial content as analytics event properties.

## Deferred capabilities and extensions

All future features remain free: receipt uploads/OCR, itemized restaurant bills,
explicit FX conversions, imports, spending insights, offline editing, payment
provider handoffs, cross-group settlement, and native clients. Current ledger
currencies and participant identities leave room for these without promising
compatibility with an unapproved future wire format. Refunds/negative expenses,
actual payment processing and billing are outside MVP. UI framework, cloud vendor,
launch market, retention policy and operating budget require later decisions.

Useful patterns to evaluate later include shareable group invitations, quick
repeat-expense templates, “who pays next” guidance and receipt itemization. These
are design inspirations rather than current competitor feature claims.
