# QA-10 exact branch-line gap ledger

Generated from the current JaCoCo XML reports by
`tools/coverage/report_branch_lines.py`. This is the source-line companion to
the method-level ledger: the 50 rows below account for all 76 missed branches.
It is discovery evidence, not closure evidence. Regenerate it after every
coverage change; a line may contain generated, defensive, or reachable
behavior, and each still requires the method-level acceptance criteria and
review status.

| Module | Package | Source | Line | Missed | Covered | Report |
| --- | --- | --- | ---: | ---: | ---: | --- |
| `app/accounts` | `com/subhrodip/squarewise/accounts/auth/abuse` | `ClientAddressResolver.kt` | 90 | 1 | 2 | `app/accounts/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/accounts` | `com/subhrodip/squarewise/accounts/auth/abuse` | `ClientAddressResolver.kt` | 93 | 1 | 1 | `app/accounts/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/accounts` | `com/subhrodip/squarewise/accounts/auth/abuse` | `ClientAddressResolver.kt` | 99 | 1 | 1 | `app/accounts/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/accounts` | `com/subhrodip/squarewise/accounts/auth/identity` | `EmailAddress.kt` | 42 | 1 | 7 | `app/accounts/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/accounts` | `com/subhrodip/squarewise/accounts/auth/identity` | `EmailAddress.kt` | 48 | 6 | 6 | `app/accounts/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/accounts` | `com/subhrodip/squarewise/accounts/auth/identity` | `EmailAddress.kt` | 51 | 4 | 4 | `app/accounts/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/accounts` | `com/subhrodip/squarewise/accounts/auth/login` | `LoginVerificationService.kt` | 75 | 1 | 1 | `app/accounts/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/accounts` | `com/subhrodip/squarewise/accounts/auth/session` | `SessionPolicy.kt` | 84 | 1 | 1 | `app/accounts/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/accounts` | `com/subhrodip/squarewise/accounts/profile/api` | `ProfileController.kt` | 82 | 4 | 0 | `app/accounts/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/accounts` | `com/subhrodip/squarewise/accounts/security` | `FallbackJwtDecoder.kt` | 37 | 1 | 1 | `app/accounts/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/bff` | `com/subhrodip/squarewise/bff/config` | `BrowserOriginPolicy.kt` | 16 | 1 | 1 | `app/bff/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/bff` | `com/subhrodip/squarewise/bff/realtime` | `LiveUpdateFanout.kt` | 102 | 1 | 1 | `app/bff/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/bff` | `com/subhrodip/squarewise/bff/realtime` | `LiveUpdateFanout.kt` | 128 | 1 | 1 | `app/bff/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/bff` | `com/subhrodip/squarewise/bff/realtime` | `LiveUpdateFanout.kt` | 162 | 1 | 3 | `app/bff/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/bff` | `com/subhrodip/squarewise/bff/transport` | `BffGatewayFilters.kt` | 26 | 1 | 3 | `app/bff/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/expenses/api` | `ExpenseController.kt` | 119 | 1 | 1 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/expenses/api` | `ExpenseController.kt` | 200 | 1 | 1 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/expenses/api` | `ExpenseController.kt` | 225 | 1 | 1 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/groups/persistence/store` | `JpaGroupStore.kt` | 84 | 1 | 1 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/groups/persistence/store` | `JpaGroupStore.kt` | 106 | 1 | 1 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/groups/persistence/store` | `JpaGroupStore.kt` | 134 | 1 | 1 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/groups/persistence/store` | `JpaGroupStore.kt` | 153 | 1 | 1 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/groups/persistence/store` | `JpaGroupStore.kt` | 167 | 1 | 1 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/groups/persistence/store` | `JpaGroupStore.kt` | 169 | 1 | 1 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/groups/persistence/store` | `JpaGroupStore.kt` | 195 | 1 | 1 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/groups/persistence/store` | `JpaGroupStore.kt` | 196 | 1 | 1 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/groups/persistence/store` | `JpaGroupStore.kt` | 206 | 1 | 1 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/groups/persistence/store` | `JpaGroupStore.kt` | 226 | 1 | 1 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/groups/persistence/store` | `JpaGroupStore.kt` | 248 | 1 | 1 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/groups/persistence/store` | `JpaGroupStore.kt` | 253 | 1 | 1 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/groups/persistence/store` | `JpaGroupStore.kt` | 275 | 1 | 1 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/groups/persistence/store` | `JpaGroupStore.kt` | 303 | 1 | 1 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/recurring/domain` | `RecurrenceSchedule.kt` | 11 | 1 | 9 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/recurring/service` | `RecurringExpenseService.kt` | 55 | 1 | 9 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/recurring/service` | `RecurringExpenseService.kt` | 123 | 1 | 9 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/recurring/service` | `RecurringExpenseService.kt` | 215 | 1 | 5 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/recurring/service` | `RecurringExpenseService.kt` | 221 | 1 | 3 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/recurring/service` | `RecurringExpenseService.kt` | 264 | 1 | 1 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/recurring/service` | `RecurringExpenseService.kt` | 309 | 2 | 8 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/recurring/service` | `RecurringExpenseService.kt` | 311 | 1 | 1 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/recurring/service` | `RecurringExpenseService.kt` | 315 | 1 | 1 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/recurring/service` | `RecurringExpenseService.kt` | 319 | 1 | 1 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/search/api` | `SearchController.kt` | 74 | 2 | 6 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/search/model` | `ExpenseSearch.kt` | 93 | 1 | 5 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/settlements/service` | `SettlementSuggestion.kt` | 53 | 1 | 1 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/expense-core` | `com/subhrodip/squarewise/expensecore/sync/api` | `SyncController.kt` | 39 | 1 | 1 | `app/expense-core/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/notifications` | `com/subhrodip/squarewise/notifications/email/delivery` | `EmailDispatcher.kt` | 46 | 2 | 2 | `app/notifications/build/reports/jacoco/test/jacocoTestReport.xml` |
| `libs/db` | `com/subhrodip/squarewise/db/health` | `DbReaderHealth.kt` | 86 | 2 | 8 | `libs/db/build/reports/jacoco/test/jacocoTestReport.xml` |
| `libs/db` | `com/subhrodip/squarewise/db/policy` | `DbOperationPolicy.kt` | 28 | 2 | 4 | `libs/db/build/reports/jacoco/test/jacocoTestReport.xml` |
| `libs/errors` | `com/subhrodip/squarewise/errors/http` | `GlobalErrorHandler.kt` | 143 | 11 | 3 | `libs/errors/build/reports/jacoco/test/jacocoTestReport.xml` |
