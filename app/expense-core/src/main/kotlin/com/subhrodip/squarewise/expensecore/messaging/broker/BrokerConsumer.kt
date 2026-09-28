package com.subhrodip.squarewise.expensecore.messaging.broker
/** Port for consuming broker messages with explicit acknowledgement. */
interface BrokerConsumer { fun receive(handler: (BrokerMessage) -> Boolean) }
