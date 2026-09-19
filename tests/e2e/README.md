# End-to-End & Chaos Test Suite

The end-to-end test suites run against the live local environment (`infra/local/docker-compose.dev.yml`) and verify complete multi-service functionality, distributed resilience, concurrency, and fault tolerance across Accounts, Expense Core, Notifications, and GraphQL BFF.

### Test Suites

1. **Product Journey Lifecycle (`test_product_journey.py`)**:
   - User profile provisioning via Accounts and GraphQL BFF (`me`).
   - Group lifecycle via GraphQL BFF (`createGroup`, `group`).
   - Invitations and membership claiming across users via Expense Core.
   - Multi-participant expense creation with equal allocation splits.
   - Real-time balance calculations, zero-sum invariant verification, and currency netting.
   - Multi-currency settlement suggestions engine via GraphQL BFF.
   - Repayments recording via GraphQL BFF (`recordRepayment`).
   - Outbox relay transactional event publishing to RabbitMQ and consumption into Notifications Inbox.
   - Offline synchronization feed snapshot & change tracking.

2. **Offline Client Sync & Replay Resilience (`test_offline_resilience.py`)**:
   - Client offline mutation queueing with client-generated UUIDs and unique `Idempotency-Key` headers.
   - Reconnect and batch replay via GraphQL BFF (`createExpense`).
   - Retransmission idempotency: identical retries succeed with 0 balance changes and 0 duplicate postings.
   - Conflicting idempotency reuse: modified payload with existing key correctly rejected with HTTP 409 (`ERR_06` / `CONFLICT`).
   - Offline sync cursor gap recovery via `/sync/changes?cursor=...`.

3. **Concurrent Member Edit Conflicts & Real-Time Invalidation (`test_concurrency_subscriptions.py`)**:
   - Real-time WebSocket connection to GraphQL BFF via RFC 6455 and `graphql-transport-ws`.
   - Subscription to `groupChanged(groupId: ID!)` with immediate delivery of revision and `changeId` invalidation events.
   - Concurrent race testing: simultaneous PUT updates to the same expense version. Exactly 1 succeeds (version increments to 2), competing update receives HTTP 409 Conflict (`ERR_06`).
   - Conflict resolution: stale client fetches latest state and reapplies cleanly.

4. **Message Broker Outage Chaos & Transactional Outbox Recovery (`test_chaos_recovery.py`)**:
   - Fault injection: pauses Expense Core and verifies GraphQL `groups` returns a
     structured upstream error without fabricated data, then verifies the
     query recovers after Expense Core is restored.
   - Fault injection: pauses RabbitMQ broker container.
   - Verifies Expense Core local ACID isolation: expense writes, balance postings, and audit entries continue to succeed without error.
   - PostgreSQL inspection: outbox records held safely in `PENDING` state.
   - Fault healing: unpauses RabbitMQ; verifies outbox relay daemon drains `PENDING` records to `PUBLISHED`.
   - End-to-end verification: Notifications service receives and confirms delivered events.

### Running Test Suites via Makefile

```sh
# Run individual test suites
make e2e-live
make e2e-offline
make e2e-concurrency
make e2e-chaos

# Run all suites in sequence
make e2e-all
```

The live HTTP helpers use a ten-second request timeout. The raw WebSocket
client uses a ten-second TCP/protocol-setup timeout and switches to blocking
event reads only after `connection_ack`; this prevents unavailable services
from hanging a test process indefinitely. These client timeouts do not claim
that application-level timeout or retry policy is production-proven.
