package com.subhrodip.pennywise.bff.messaging.transport

import com.subhrodip.pennywise.bff.messaging.model.BffEventEnvelope
import com.subhrodip.pennywise.bff.messaging.service.BffEventConsumer

import com.rabbitmq.client.Channel
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener
import tools.jackson.databind.ObjectMapper

/**
 * AMQP message listener adapter for BFF event consumption.
 *
 * Deserializes message payloads conforming to `envelope.schema.json`, delegates to
 * [BffEventConsumer] for deduplication and live-update fanout, and handles channel
 * acknowledgements (acknowledging valid and duplicate messages, rejecting poison-pill messages
 * without requeue).
 */
class RabbitBffEventListener(
    private val eventConsumer: BffEventConsumer,
    private val objectMapper: ObjectMapper
) : ChannelAwareMessageListener {

    private val log = LoggerFactory.getLogger(RabbitBffEventListener::class.java)

    override fun onMessage(message: Message, channel: Channel?) {
        val deliveryTag = message.messageProperties.deliveryTag
        try {
            val envelope = objectMapper.readValue(message.body, BffEventEnvelope::class.java)
            eventConsumer.consume(envelope)
            channel?.basicAck(deliveryTag, false)
        } catch (ex: Exception) {
            log.error("Failed to process message with deliveryTag $deliveryTag: ${ex.message}. Rejecting poison pill without requeue.", ex)
            try {
                // Reject without requeue to prevent poisoning the temporary fanout queue
                channel?.basicReject(deliveryTag, false)
            } catch (ackEx: Exception) {
                log.error("Failed to reject message with deliveryTag $deliveryTag: ${ackEx.message}", ackEx)
            }
        }
    }
}
