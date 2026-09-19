# OPS-24 - Harden lightweight and E2E CI portability

## Objective

Keep hosted verification self-contained: lightweight repository checks must use
the declared Python/uv toolchain, and the E2E job must have exactly one owner for
its PostgreSQL and RabbitMQ containers and host ports.

## Dependencies

- `OPS-20`

## Owned paths

- `.github/workflows/`
- `Makefile`
- `docs/operations/`
- `docs/tasks/details/OPS-24.md`
- `docs/tasks/progress.md`

## Implementation design

- Run workflow syntax validation through a pinned, ephemeral `yamllint` command
  after the existing `setup-uv` step. Do not assume Ruby is installed on slim
  GitHub-hosted runners.
- Let `infra/local/docker-compose.dev.yml` exclusively own the E2E PostgreSQL and
  RabbitMQ containers. The E2E job must not also declare GitHub Actions service
  containers on host ports `5432` and `5672`.
- Keep the verification matrix service containers unchanged because those jobs
  run Gradle modules directly rather than the complete Compose stack.

## Validation commands

- `make workflow-validate`
- `make compose-config`
- `python3 tools/contracts/validate.py`
- `python3 tools/contracts/validate_public_surface.py`
- `git diff --check`

## Expected evidence

- All workflow files parse without Ruby.
- Every Compose topology renders successfully.
- E2E startup has no duplicate job-level host-port owner.

## Status

Complete. Lightweight CI initializes `uv` and runs pinned `yamllint` instead of
assuming Ruby is installed. E2E no longer declares Actions-level PostgreSQL and
RabbitMQ services, so the full Compose topology is the single owner of those
containers and host ports.

## Verification evidence

- Contract validation passed for all JSON, GraphQL, and 140 task links.
- Public-surface validation passed for 41 REST operations, 9 GraphQL root
  operations, and the Bruno request inventory.
- All six local Compose files rendered successfully with `docker compose config
  --quiet`.
- The four workflow files parsed with the locally available PyYAML parser.
- `git diff --check` passed.
- Exact `make workflow-validate` execution was unavailable because this Windows
  shell does not have `uvx`; a Docker-hosted `uvx` attempt was also unavailable
  because the Docker daemon was not running. Hosted CI installs `uv` explicitly
  before invoking the target.
