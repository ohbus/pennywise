package com.subhrodip.pennywise.notifications.consumer.transport
import com.subhrodip.pennywise.notifications.consumer.model.NotificationEvent
import com.subhrodip.pennywise.notifications.consumer.service.NotificationConsumer

import com.rabbitmq.client.Channel
import java.util.UUID
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener
import org.springframework.stereotype.Component

/** Converts a validated broker envelope to the notification consumer event. */
fun BrokerEnvelope.toNotificationEvent(): NotificationEvent {
    val notificationId = (payload["notificationId"] as? String)?.let {
        runCatching { UUID.fromString(it) }.getOrNull()
    } ?: aggregateId
    val subject = (payload["subject"] as? String
        ?: payload["recipient"] as? String
        ?: payload["recipientId"] as? String
        ?: payload["userId"] as? String)
        ?.takeIf { it.isNotBlank() }
        ?: groupId.toString()
    val message = (payload["message"] as? String ?: payload["description"] as? String)
        ?.takeIf { it.isNotBlank() }
        ?: "$eventType for group $groupId"
    return NotificationEvent(
        eventId = eventId,
        notificationId = notificationId,
        subject = subject.take(200),
        eventType = eventType.take(200),
        message = message.take(2000),
        occurredAt = occurredAt
    )
}

/** RabbitMQ adapter that acknowledges, rejects, or requeues notification events. */
@Component
class RabbitNotificationListener(
    private val consumer: NotificationConsumer,
    private val envelopeParser: BrokerEnvelopeParser
) : ChannelAwareMessageListener {
    @RabbitListener(
        queues = ["\${pennywise.notifications.queue:pennywise.notifications.v2}"],
        ackMode = "MANUAL"
    )
    override fun onMessage(message: Message, channel: Channel?) {
        val deliveryTag = message.messageProperties.deliveryTag
        try {
            consumer.consume(envelopeParser.parse(message.body).toNotificationEvent())
            channel?.basicAck(deliveryTag, false)
        } catch (_: InvalidEnvelopeException) {
            channel?.basicReject(deliveryTag, false)
        } catch (_: Throwable) {
            channel?.basicReject(deliveryTag, message.messageProperties.redelivered != true)
        }
    }
}
