package com.subhrodip.pennywise.expensecore.messaging

import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertThrows
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class OutboxRelayDaemonTest {

    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(OutboxMessagingConfiguration::class.java)
        .withBean(OutboxStore::class.java, { OutboxRelay() })

    @Test
    fun `deployed outbox configuration requires durable publishing`() {
        assertThrows(IllegalArgumentException::class.java) {
            RequiredOutboxConfiguration(OutboxRelayProperties())
        }
        assertThrows(IllegalArgumentException::class.java) {
            RequiredOutboxConfiguration(OutboxRelayProperties(enabled = true))
        }
        RequiredOutboxConfiguration(OutboxRelayProperties(enabled = true, rabbitEnabled = true))
    }

    @Test
    fun `daemon is active and publishes available messages when enabled`() {
        contextRunner
            .withPropertyValues(
                "pennywise.outbox.enabled=true",
                "pennywise.outbox.batch-size=10",
                "pennywise.outbox.lease-seconds=15"
            )
            .withBean(OutboxRelayDaemon::class.java)
            .run { context ->
                assertTrue(context.containsBean("outboxRelayDaemon"))
                val daemon = context.getBean(OutboxRelayDaemon::class.java)
                val relay = context.getBean(OutboxStore::class.java)

                val eventId = UUID.randomUUID()
                val message = OutboxMessage(
                    eventId = eventId,
                    eventType = "expense.created",
                    aggregateId = UUID.randomUUID(),
                    groupId = UUID.randomUUID(),
                    groupRevision = 1,
                    occurredAt = Instant.now(),
                    payload = mapOf("amount" to 500)
                )
                relay.append(message)

                val result = daemon.pollAndPublish()
                assertEquals(1, result.claimed)
                assertEquals(1, result.confirmed)
                assertEquals(0, result.rejected)

                val snapshot = relay.snapshot()
                assertEquals(OutboxStatus.PUBLISHED, snapshot.single().status)
            }
    }

    @Test
    fun `daemon bean is omitted when disabled by default`() {
        contextRunner
            .run { context ->
                org.junit.jupiter.api.Assertions.assertThrows(NoSuchBeanDefinitionException::class.java) {
                    context.getBean(OutboxRelayDaemon::class.java)
                }
            }
    }
}
