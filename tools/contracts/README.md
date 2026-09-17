# Contract validation

Run `python3 tools/contracts/validate.py` for the dependency-free check. The
quality task may add OpenAPI/GraphQL schema validators after pinning their exact
versions. This script intentionally checks repository invariants without making
the backend dependent on Node or a hosted contract service.
