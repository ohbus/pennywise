package com.subhrodip.pennywise.notifications.consumer

import com.subhrodip.pennywise.ids.EventConstants
import com.subhrodip.pennywise.notifications.delivery.DeliveryRateLimiter
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Declarables
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Declares the durable broker resources consumed by Notifications.
 *
 * The declarations are idempotent and allow a clean local or production broker
 * to be started without an out-of-band queue provisioning step.
 */
@ConfigurationProperties(prefix = "pennywise.notifications")
data class NotificationMessagingProperties(
    /** Queue receiving notification events. */
    var queue: String = "pennywise.notifications.v2",
    /** Queue receiving encrypted passwordless authentication-email events. */
    var authEmailQueue: String = "pennywise.auth-email.v2",
    /** Exchange receiving messages rejected as poison or permanently failed. */
    var deadLetterExchange: String = "pennywise.events.dlx",
    /** Queue retaining rejected notification events for operator inspection. */
    var deadLetterQueue: String = "pennywise.notifications.v2.dlq",
    /** Queue retaining rejected authentication-email events. */
    var authEmailDeadLetterQueue: String = "pennywise.auth-email.v2.dlq"
)

/** Declares the notification event exchange, queues, and domain-event bindings. */
@Configuration
@EnableConfigurationProperties(NotificationMessagingProperties::class)
class NotificationMessagingConfiguration {

    /** Provides the bounded per-recipient delivery policy used by consumers. */
    @Bean
    fun deliveryRateLimiter(): DeliveryRateLimiter = DeliveryRateLimiter()

    /** Declares the shared durable event exchange. */
    @Bean
    fun notificationEventsExchange(): TopicExchange =
        TopicExchange(EventConstants.EVENTS_EXCHANGE, true, false)

    /** Declares the durable notification consumer queue. */
    @Bean
    fun notificationQueue(properties: NotificationMessagingProperties): Queue =
        Queue(properties.queue, true, false, false, mapOf("x-dead-letter-exchange" to properties.deadLetterExchange))

    /** Routes group events to the notification consumer queue. */
    @Bean
    fun notificationGroupBinding(
        notificationQueue: Queue,
        notificationEventsExchange: TopicExchange
    ): Binding = BindingBuilder.bind(notificationQueue)
        .to(notificationEventsExchange)
        .with(EventConstants.Routing.ALL_GROUP_EVENTS)

    /** Routes expense events to the notification consumer queue. */
    @Bean
    fun notificationExpenseBinding(
        notificationQueue: Queue,
        notificationEventsExchange: TopicExchange
    ): Binding = BindingBuilder.bind(notificationQueue)
        .to(notificationEventsExchange)
        .with(EventConstants.Routing.ALL_EXPENSE_EVENTS)

    /** Routes settlement events to the notification consumer queue. */
    @Bean
    fun notificationSettlementBinding(
        notificationQueue: Queue,
        notificationEventsExchange: TopicExchange
    ): Binding = BindingBuilder.bind(notificationQueue)
        .to(notificationEventsExchange)
        .with(EventConstants.Routing.ALL_SETTLEMENT_EVENTS)

    /** Declares the dedicated durable auth-email queue. */
    @Bean
    fun authEmailQueue(properties: NotificationMessagingProperties): Queue =
        Queue(properties.authEmailQueue, true, false, false, mapOf("x-dead-letter-exchange" to properties.deadLetterExchange))

    /** Declares the exchange receiving permanently rejected deliveries. */
    @Bean
    fun notificationDeadLetterExchange(properties: NotificationMessagingProperties): TopicExchange =
        TopicExchange(properties.deadLetterExchange, true, false)

    /** Declares the durable notification poison-message queue. */
    @Bean
    fun notificationDeadLetterQueue(properties: NotificationMessagingProperties): Queue =
        Queue(properties.deadLetterQueue, true)

    /** Declares the durable authentication-email poison-message queue. */
    @Bean
    fun authEmailDeadLetterQueue(properties: NotificationMessagingProperties): Queue =
        Queue(properties.authEmailDeadLetterQueue, true)

    /** Routes rejected notification events to the notification DLQ. */
    @Bean
    fun notificationDeadLetterBinding(
        notificationDeadLetterQueue: Queue,
        notificationDeadLetterExchange: TopicExchange
    ): Binding = BindingBuilder.bind(notificationDeadLetterQueue)
        .to(notificationDeadLetterExchange)
        .with("#")

    /** Routes rejected auth-email events to the auth-email DLQ. */
    @Bean
    fun authEmailDeadLetterBinding(
        authEmailDeadLetterQueue: Queue,
        notificationDeadLetterExchange: TopicExchange
    ): Binding = BindingBuilder.bind(authEmailDeadLetterQueue)
        .to(notificationDeadLetterExchange)
        .with(EventConstants.Routing.AUTH_EMAIL_REQUESTED)

    /** Explicitly declares poison queues and their bindings as one broker topology. */
    @Bean
    fun deadLetterDeclarables(properties: NotificationMessagingProperties): Declarables {
        val exchange = TopicExchange(properties.deadLetterExchange, true, false)
        val notificationQueue = Queue(properties.deadLetterQueue, true)
        val authEmailQueue = Queue(properties.authEmailDeadLetterQueue, true)
        return Declarables(
            exchange,
            notificationQueue,
            authEmailQueue,
            BindingBuilder.bind(notificationQueue).to(exchange).with("#"),
            BindingBuilder.bind(authEmailQueue).to(exchange).with(EventConstants.Routing.AUTH_EMAIL_REQUESTED)
        )
    }

    /** Routes only versioned auth-email events to the protected delivery adapter. */
    @Bean
    fun authEmailBinding(
        authEmailQueue: Queue,
        notificationEventsExchange: TopicExchange
    ): Binding = BindingBuilder.bind(authEmailQueue)
        .to(notificationEventsExchange)
        .with(EventConstants.Routing.AUTH_EMAIL_REQUESTED)
}
