package com.subhrodip.pennywise.bff.config

import graphql.analysis.MaxQueryComplexityInstrumentation
import graphql.analysis.MaxQueryDepthInstrumentation
import graphql.execution.instrumentation.Instrumentation
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Configures bounded GraphQL execution to limit resource-amplifying queries. */
@ConfigurationProperties(prefix = "pennywise.bff.graphql")
data class GraphQlAbuseProperties(
    /** Maximum parsed query depth accepted by the execution engine. */
    var maxDepth: Int = 8,
    /** Maximum calculated field complexity accepted by the execution engine. */
    var maxComplexity: Int = 100,
    /** Maximum number of subscriptions one authenticated principal may hold. */
    var maxSubscriptionsPerUser: Int = 20,
    /** Maximum queued live updates retained per subscription. */
    var subscriptionQueueCapacity: Int = 64,
    /** Maximum lifetime of one live subscription, in seconds. */
    var subscriptionTtlSeconds: Long = 1800
)

/** Installs GraphQL Java depth and complexity guards before resolver execution. */
@Configuration
@EnableConfigurationProperties(GraphQlAbuseProperties::class)
class GraphQlAbuseConfiguration {
    /** Rejects deeply nested GraphQL operations. */
    @Bean
    fun graphQlDepthInstrumentation(properties: GraphQlAbuseProperties): Instrumentation =
        MaxQueryDepthInstrumentation(properties.maxDepth)

    /** Rejects operations whose calculated field cost exceeds the configured bound. */
    @Bean
    fun graphQlComplexityInstrumentation(properties: GraphQlAbuseProperties): Instrumentation =
        MaxQueryComplexityInstrumentation(properties.maxComplexity)
}
