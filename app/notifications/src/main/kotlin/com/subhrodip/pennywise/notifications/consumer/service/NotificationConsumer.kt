package com.subhrodip.pennywise.notifications.consumer.service
import com.subhrodip.pennywise.notifications.consumer.model.NotificationConsumptionOutcome
import com.subhrodip.pennywise.notifications.consumer.model.NotificationEvent

/** Port for applying broker notification events. */
fun interface NotificationConsumer { fun consume(event: NotificationEvent): NotificationConsumptionOutcome }
