package com.subhrodip.pennywise.notifications

import com.subhrodip.pennywise.ids.EventConstants
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
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
    var queue: String = "pennywise.notifications",
    /** Queue receiving encrypted passwordless authentication-email events. */
    var authEmailQueue: String = "pennywise.auth-email"
)

/** Declares the notification event exchange, queues, and domain-event bindings. */
@Configuration
@EnableConfigurationProperties(NotificationMessagingProperties::class)
class NotificationMessagingConfiguration {

    /** Declares the shared durable event exchange. */
    @Bean
    fun notificationEventsExchange(): TopicExchange =
        TopicExchange(EventConstants.EVENTS_EXCHANGE, true, false)

    /** Declares the durable notification consumer queue. */
    @Bean
    fun notificationQueue(properties: NotificationMessagingProperties): Queue =
        Queue(properties.queue, true, false, false)

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
        Queue(properties.authEmailQueue, true, false, false)

    /** Routes only versioned auth-email events to the protected delivery adapter. */
    @Bean
    fun authEmailBinding(
        authEmailQueue: Queue,
        notificationEventsExchange: TopicExchange
    ): Binding = BindingBuilder.bind(authEmailQueue)
        .to(notificationEventsExchange)
        .with(EventConstants.Routing.AUTH_EMAIL_REQUESTED)
}
