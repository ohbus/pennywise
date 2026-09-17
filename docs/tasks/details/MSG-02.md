# MSG-02 — Deliver committed group changes to every BFF replica

Publish committed group-change events from Expense Core and consume them through
per-replica BFF queues into live-update fanout with deduplication and resync-safe
semantics. Depends on CORE-22, MSG-01, and BFF-02. Validate two-replica delivery,
reconnect recovery, and broker failure behavior.
