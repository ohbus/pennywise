# Kotlin and Python coding guidelines

The mandatory principle policy is documented in
[programming-principles.md](programming-principles.md). Review every change
against that policy before treating a task as complete.

Pennywise follows the Kotlin conventions in this document and uses Gradle
checks as the current quality baseline. Spotless 8.1.0 provides the compatible
Kotlin formatting check for the approved baseline without rewriting existing
layout. A repository-wide migration remains separately scoped. Run `make lint`
before committing; it validates contracts, the Spotless baseline, and Gradle
checks. The incompatible ktlint integration is not used.

- Use explicit imports; do not use fully qualified names in implementation code.
- Validate every request DTO at the boundary.
- Keep entities and repositories inside their owning service. Enforce strict SOLID file separation: never combine JPA `@Entity` definitions, Spring Data `@Repository` interfaces, and `@Service` or store adapter implementations in the same file. Each class or interface must have its own dedicated source file.
- Use minor-unit integers or strings for money.
- Test observable behavior and never commit generated files or credentials.
- Prefer cohesive modules, narrow interfaces, composition, and dependency
  injection. Do not add speculative abstractions or duplicate business rules.
- Keep commands and queries distinct, make failure behavior explicit, and
  leave touched code easier for the next maintainer to understand.
- Write structured Javadoc / KDoc doc comments (`/** ... */`) for all public
  classes, interfaces, functions, endpoints, and domain models, documenting intent,
  parameters, return values, invariants, and failure modes.
- Keep repository documentation synchronized with implementation as tasks are executed;
  never leave documentation updates for a later stage when changing behavior.
- Every delegated subagent must thoroughly review all relevant documentation
  and adhere strictly to project standards before writing any code.

## Python typing

All Python source, test, and operational code is strongly typed. Functions and
methods require parameter and return annotations, collections use explicit
generic element types, and structured JSON should use typed mappings/models
where practical. `Any` is reserved for genuinely untyped external payloads and
must be narrowed at the boundary. Use `uv`/`uvx` for isolated Python tooling;
do not install packages into the system interpreter. Run
`make python-typecheck`; CI runs the same isolated mypy configuration. Do not
bypass the gate with `# type: ignore` without an adjacent reason and a removal
condition.
