# Production validation follow-up

QA-08 tracks evidence that cannot be established by the local Docker stack.
It is deliberately separate from QA-07’s executable local public-interface
coverage.

| Dimension | Required evidence | Current status |
|---|---|---|
| Capacity and latency | Production-like load profile, thresholds, p95/p99 artifacts | Open |
| Failover and restore | Database/broker/service recovery rehearsal with data-integrity proof | Open |
| Security | Dependency, image, secret, and API security scans | Open |
| Rollback | Deployment rollback rehearsal and post-rollback verification | Open |
| WebSocket protocol | Malformed frames, duplicate IDs, backpressure, replay-after-reconnect, timeout/retry semantics | Open; local handshake, authorization, reconnect, unsubscribe, and delivery are covered by QA-07 |

Local contract, unit, live-integration, and E2E results must not be promoted to
production evidence. QA-08 may close a row only when the artifact, environment,
threshold, and procedure are recorded.
