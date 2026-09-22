# CQRS data-access contract

Pennywise uses explicit command/query policies at the application boundary. A
command, lock, claim, migration, reconciliation, or strong financial read uses
the writer. Only an explicitly approved query may use a named reader pool.
There is no blanket GET or read-only routing rule.

## Consistency levels

- `STRONG`: read from the writer; required for authorization, financial state,
  idempotency, revisions, and any operation immediately following a mutation.
- `SESSION`: read from a reader only after the caller's writer watermark has
  been replayed; otherwise use the writer.
- `BOUNDED_STALENESS`: use a reader only while its measured replay lag is inside
  the operation's budget; a stale or unavailable reader uses the bounded writer
  fallback when the operation explicitly permits it.
- `EVENTUAL`: an approved historical/read-model query may use a healthy reader;
  a failed reader may use bounded writer fallback. The fallback is never used
  for commands or strong reads.

## Writer-only invariants

Financial postings, balances, settlements, group membership authorization,
claims, idempotency records, revision allocation, outbox writes, delivery
leases/attempts, authentication/session/rate-limit state, migrations, and
reconciliation all remain writer-bound. Reader pools are read-only credentials
and are not used by Flyway or Hibernate schema validation.

## Failure and freshness semantics

Reader health is explicit: healthy, lagging, disconnected, or circuit-open.
Repeated connection failures open a bounded circuit; recovery requires a
successful health probe or operation. A query that does not permit fallback
fails rather than silently weakening consistency. Route, lag, pool, fallback,
and circuit outcomes must be observable before a capability is promoted.

The initial local topology is diagnostic: `infra/local/docker-compose.replica.yml`
adds an asynchronous PostgreSQL streaming replica, while the default Compose
profile remains single-database writer-only mode.
