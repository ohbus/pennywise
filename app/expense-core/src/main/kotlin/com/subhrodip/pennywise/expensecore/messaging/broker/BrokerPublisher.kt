package com.subhrodip.pennywise.expensecore.messaging.broker
/** Port for publishing broker messages. */
fun interface BrokerPublisher { fun publish(message: BrokerMessage): PublishResult }
