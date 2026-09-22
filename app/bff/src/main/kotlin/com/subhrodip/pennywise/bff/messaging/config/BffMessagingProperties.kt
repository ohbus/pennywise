package com.subhrodip.pennywise.bff.messaging.config

import com.subhrodip.pennywise.ids.events.EventConstants
import org.springframework.boot.context.properties.ConfigurationProperties

/** Configuration properties controlling BFF replica event fanout. */
@ConfigurationProperties(prefix = "pennywise.bff.messaging")
data class BffMessagingProperties(
    var enabled: Boolean = false,
    var exchange: String = EventConstants.EVENTS_EXCHANGE,
    var routingKey: String = EventConstants.Routing.ALL_EVENTS,
    var deduplicatorCapacity: Int = 10_000
)
