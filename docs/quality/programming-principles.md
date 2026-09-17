# Programming principles

These principles are mandatory design and review criteria for new code and
meaningful changes. Prefer the smallest clear solution that satisfies a
registered task and its contracts.

## Simplicity and scope

- **KISS / simplest thing:** choose the smallest design that solves the stated problem.
- **Write/generate minimal code:** write as little new code as possible; reuse existing libraries, components, and patterns to minimize maintenance surface.
- **Cognitive simplicity & human readability:** keep code straightforward, concise, and mentally lightweight to read, understand, and review.
- **YAGNI:** do not implement forecasted features or abstractions without a current consumer or documented contract.
- **No premature optimization:** measure a real bottleneck before trading clarity for speed.

## Boundaries and behavior

- **Separation of concerns, SRP, and Curly's Law:** each module, class, file, and function has one coherent responsibility. In enterprise code, persistent entities (`@Entity`), Spring Data repositories (`@Repository`), and business services or persistence store adapters (`@Service`) must never share a single file; each belongs in its own dedicated source file.
- **Cohesion and low coupling:** keep related behavior together and expose the narrowest interface; apply the Law of Demeter.
- **Orthogonality:** unrelated concerns must not share accidental state, dependencies, or lifecycle rules.
- **Information hiding and encapsulated variation:** hide implementation details and isolate likely changes behind small interfaces.
- **Composition over inheritance:** use composition unless substitutability is explicit and tested.
- **IoC and dependency inversion:** domain behavior depends on contracts; frameworks and infrastructure are injected at boundaries.
- **ISP, LSP, and OCP:** keep interfaces client-specific, preserve subtype behavior, and extend stable policies through explicit seams.

## Knowledge and maintenance

- **DRY / single source of truth:** represent each business rule, version, and contract once; derive consumers where practical.
- **Command-query separation:** commands change state; queries return state; do not hide mutations in read methods.
- **Robustness / tolerant readers:** emit precise contract-compliant data and accept harmless additive input without weakening semantic validation.
- **Maintainer-first and Boy Scout rule:** make names, errors, tests, and docs explain intent; leave touched code clearer than it was.

## Engineering reality

Expect failure (Murphy), recognize that adding people can increase coordination cost (Brooks), and make review evidence visible so more eyes can find shallow bugs (Linus). These are planning heuristics, not excuses to skip testing or ownership.

## Review questions

Before merging, ask: What is the simplest working design? What current requirement justifies each abstraction? Where is the single source of truth? Which concern owns this behavior? What is the contract for invalid or additive input? Is the change easy for the next maintainer to test and replace?
