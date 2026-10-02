# QA-10 Concrete Execution-Gap Ledger

Generated from the current JaCoCo reports. This inventory contains concrete
production methods with zero covered instructions after excluding interfaces,
compiler-generated methods, accessors, and application entry points. It is a
review backlog, not permission to delete implementation or weaken contracts.

## Exact current records

| Module | Production class | Source | Method | Line | Missed instructions | QA row | Acceptance criteria | Status | Next action | Report |
| --- | --- | --- | --- | ---: | ---: | --- | --- | --- | --- | --- |
| `app/accounts` | `com/subhrodip/squarewise/accounts/profile/api/ProfileController` | `ProfileController.kt` | `problem` | 63 | 35 | `QA10-A06` | Subject-scoped profile/deletion/export authorization, exact errors, durable state, and redaction. | **NO-INSTRUCTION-EXECUTION** | ProfileControllerTest covers the public profile/deletion/export problem behavior, but source search found no production caller for this private helper. Keep the helper as an OPEN-DESIGN item until a real public failure is routed through it or a separately reviewed cleanup decision is made; do not use reflection-only coverage or delete it to change JaCoCo. | `app/accounts/build/reports/jacoco/test/jacocoTestReport.xml` |
| `app/accounts` | `com/subhrodip/squarewise/accounts/profile/api/ProfileController` | `ProfileController.kt` | `mapErrorCode` | 82 | 15 | `QA10-A06` | Subject-scoped profile/deletion/export authorization, exact errors, durable state, and redaction. | **NO-INSTRUCTION-EXECUTION** | ProfileControllerTest covers the public profile/deletion/export problem behavior, but source search found no production caller for this private helper. Keep the helper as an OPEN-DESIGN item until a real public failure is routed through it or a separately reviewed cleanup decision is made; do not use reflection-only coverage or delete it to change JaCoCo. | `app/accounts/build/reports/jacoco/test/jacocoTestReport.xml` |
| `libs/observability` | `com/subhrodip/squarewise/observability/db/DbTelemetry` | `DbTelemetry.kt` | `measureQuery` | 57 | 22 | `QA10-E05` | Bounded observability labels and exact success/failure/slow/fallback metric behavior. | **INLINE-EXPANDED** | Retain direct behavior tests at call sites; Kotlin inline expansion does not execute this JaCoCo method node, so do not add reflection-only coverage or change the contract. | `libs/observability/build/reports/jacoco/test/jacocoTestReport.xml` |
