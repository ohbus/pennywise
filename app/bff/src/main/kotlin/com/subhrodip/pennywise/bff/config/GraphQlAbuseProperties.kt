package com.subhrodip.pennywise.bff.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** Configuration bounds for GraphQL query cost and realtime subscriptions. */
@ConfigurationProperties(prefix = "pennywise.bff.graphql")
data class GraphQlAbuseProperties(
    var maxDepth: Int = 8,
    var maxComplexity: Int = 100,
    var maxSubscriptionsPerUser: Int = 20,
    var subscriptionQueueCapacity: Int = 64,
    var subscriptionTtlSeconds: Long = 1800
)
