# E2E test boundary

End-to-end tests run against the Docker Compose development stack after product
endpoints exist. Each scenario must start from isolated containers, seed through
public APIs, and verify behavior through the BFF, never through service tables.

Required scenarios are tracked in `docs/quality/acceptance.md`: create a group,
invite/claim a participant, add and edit an expense, settle up, disconnect and
replay an offline creation, and recover a WebSocket gap through the change feed.

The scaffold phase intentionally has no public product endpoints, so no fake E2E
green result is recorded. The first API implementation task must add the runner
and attach reports to QA-01.
