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

## Brand Identity Assets

The canonical brand artwork files and convenient generic symlinks are organized in this directory:

- Canonical source files:
  - [`squarewise-logo.svg`](squarewise-logo.svg): Primary horizontal brand logo featuring the Squarewise wordmark and overlapping square icon badge (1200x320 viewBox).
  - [`squarewise-icon.svg`](squarewise-icon.svg): Standalone square icon / app icon / favicon featuring overlapping rounded squares in vibrant blue, teal, and navy (512x512 viewBox).
- Generic stable references (symlinks within this directory):
  - [`logo.svg`](logo.svg) &rarr; `squarewise-logo.svg`
  - [`icon.svg`](icon.svg) &rarr; `squarewise-icon.svg`

Documentation and UI references target the generic names located in `docs/visuals/` (`docs/visuals/logo.svg` / `docs/visuals/icon.svg`), allowing brand artwork or color variations to be refreshed seamlessly by re-pointing symlinks without polluting the repository root.

<p align="center">
  <img src="logo.svg" alt="Squarewise Logo" width="360">&nbsp;&nbsp;&nbsp;&nbsp;
  <img src="icon.svg" alt="Squarewise Icon" width="80">
</p>

Edit the diagram source and the relevant architecture or contract document in
the same task. Do not edit generated SVG files. Render locally with:

```bash
make docs-diagrams
```

The renderer is defined in [`infra/docs/docker-compose.yml`](../../infra/docs/docker-compose.yml)
and writes generated SVGs to `build/docs/diagrams/`, which is ignored by Git.
