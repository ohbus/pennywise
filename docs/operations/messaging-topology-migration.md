# Messaging topology migration

The Notifications release adds `x-dead-letter-exchange=pennywise.events.dlx`
to the durable source queues. RabbitMQ does not permit changing queue arguments
by redeclaring an existing queue; an older queue without that argument causes
`PRECONDITION_FAILED` and prevents the listener container from starting.

Before deploying this release, drain or otherwise account for messages in the
source queues, stop consumers, and recreate the source queues with the current
application topology. The exact procedure must be performed by the broker
operator with the deployment's approved backup/retention process; never run a
blind delete in production.

After recreation, verify:

```text
pennywise.notifications.v2       durable, DLX pennywise.events.dlx
pennywise.auth-email.v2          durable, DLX pennywise.events.dlx
pennywise.notifications.v2.dlq   durable
pennywise.auth-email.v2.dlq      durable
```

The `.v2` defaults intentionally avoid redeclaring legacy queues whose
arguments cannot be changed in place. Operators must drain and account for
legacy `pennywise.notifications` and `pennywise.auth-email` queues, then
replay retained messages into the versioned queues according to the approved
release procedure.

Then publish a deliberately malformed notification envelope in a controlled
environment and verify it is rejected without requeue and appears in the
notification DLQ. A transient consumer failure must be requeued according to
the configured retry policy. Record queue arguments, bindings, message counts,
and the test timestamp in release evidence.
