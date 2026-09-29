# Squarewise visual documentation

The `.mmd` files in this directory are the source of truth for visual
documentation. They are intentionally text-based so architecture changes can be
reviewed in Git alongside the contracts and task registry.

The diagrams cover three levels:

- `high-level-architecture.mmd` shows users, the GraphQL BFF, REST services, and
  infrastructure dependencies.
- `low-level-expense-flow.mmd` shows the expense write path, atomic financial
  transaction, outbox publication, and notification delivery.
- `component-boundaries.mmd` shows deployable application boundaries, shared
  libraries, owned persistence, and prohibited direct cross-service access.
- `auth-lifecycle-unauthenticated.mmd` shows the authentication and session
  establishment flow for unauthenticated users (login initiation, credential delivery, and verification).
- `auth-lifecycle-authenticated.mmd` shows the authorization, JWT validation,
  token family refresh rotation, and logout lifecycle for authenticated users.

Edit the diagram source and the relevant architecture or contract document in
the same task. Do not edit generated SVG files. Render locally with:

```bash
make docs-diagrams
```

The renderer is defined in [`infra/docs/docker-compose.yml`](../../infra/docs/docker-compose.yml)
and writes generated SVGs to `build/docs/diagrams/`, which is ignored by Git.
