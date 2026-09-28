package com.subhrodip.squarewise.expensecore.messaging.broker
/** Port for publishing broker messages. */
fun interface BrokerPublisher { fun publish(message: BrokerMessage): PublishResult }
