package com.subhrodip.pennywise.bff.config

import graphql.analysis.MaxQueryComplexityInstrumentation
import graphql.analysis.MaxQueryDepthInstrumentation
import graphql.execution.instrumentation.Instrumentation
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

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
