# 1M-user production capacity baseline

This is a sizing and verification baseline, not a capacity guarantee. It assumes
1,000,000 registered users and must be re-measured with production-like data,
networking, database hardware, and deployment topology before launch.

## Workload model

| Input | Baseline |
| --- | ---: |
| Registered users | 1,000,000 |
| Monthly active users | 350,000 |
| Daily active users | 100,000 |
| Peak-hour active users | 25,000 |
| Peak concurrent requests | 2,000 |
| Peak sustained request rate | 1,000 requests/s |
| 5-minute burst | 1,500 requests/s |
| Read/write mix | 90% / 10% |
| Notification events at peak | 100 events/s |
| Availability target | 99.9% monthly for public API |

The peak rate is an engineering starting point: it must be replaced with
observed traffic multiplied by a documented growth and burst factor. It does
not mean one million users are simultaneously online.

## Per-service acceptance targets

At the stated peak, with three or more application replicas per service:

| Surface | Sustained rate | p95 | p99 | Error rate |
| --- | ---: | ---: | ---: | ---: |
| Accounts REST | 150 req/s | <250 ms | <500 ms | <0.1% |
| Expense Core reads | 400 req/s | <400 ms | <800 ms | <0.1% |
| Expense Core writes | 50 req/s | <600 ms | <1,200 ms | <0.1% |
| Notifications REST | 100 req/s | <300 ms | <600 ms | <0.1% |
| BFF GraphQL | 300 req/s | <500 ms | <1,000 ms | <0.1% |

The rates sum to the modeled 1,000 requests/s. Authentication, upstream fanout,
database saturation, broker backlog, and JVM memory must remain below alert
thresholds throughout a 60-minute soak and a 1,500 requests/s 10-minute burst.

## Initial infrastructure envelope

This envelope is a starting point for a capacity test, not a final purchase
order:

- Three application replicas per service, each 4 vCPU / 8 GiB RAM, with autoscaling
  from 3 to 12 replicas on CPU, request latency, and in-flight work.
- PostgreSQL primary at 8 vCPU / 32 GiB RAM with provisioned IOPS, one synchronous
  standby for failover, and read replicas only after measured query pressure.
- Connection pool limit 40 per application replica; total database connections
  must remain below 70% of the database limit.
- RabbitMQ three-node quorum cluster, durable queues, publisher confirms, and
  consumer capacity for at least 300 events/s sustained and 1,000 events/s burst.
- Prometheus retention and cardinality limits sized separately from application
  storage; never use request IDs, users, groups, or exception text as labels.

The repository profile (`tests/load/k6/one-million-baseline.js`) exercises real
fixture-backed expense writes with unique idempotency keys, setup/teardown group
archival, and post-run financial reconciliation via `make load-mutation-check` (OPS-23).

## Local development environment boundary vs. production envelope

Local runs on a single Docker host (such as developer workstations) share CPU, memory,
and I/O across all four applications, PostgreSQL, RabbitMQ, Mailpit, and Prometheus/Grafana.
Under concurrent 1,000 req/s loads, single-instance local containers experience port and
connection-pool contention, causing p95 latency to exceed production targets while
maintaining 0.00% HTTP failure rate. This local saturation limit is documented as an
environment ceiling, not a defect in architecture or code. Production validation requires
deploying the multi-replica infrastructure envelope defined above.

## Required evidence before claiming readiness

Run the k6 1M profile against a production-like environment with representative
row counts and replica counts. Record throughput, p50/p95/p99, errors, dropped
iterations, CPU, heap, GC, connection pools, PostgreSQL locks/IOPS, RabbitMQ
queue depth, outbox age, and recovery time. Pass only if the service targets,
availability target, and dependency saturation limits remain green during soak,
burst, one-replica loss, broker delay, and database failover drills.
