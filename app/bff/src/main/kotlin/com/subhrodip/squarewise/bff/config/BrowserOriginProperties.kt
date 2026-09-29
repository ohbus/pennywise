package com.subhrodip.squarewise.bff.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** Exact browser origins permitted to call the BFF GraphQL transport. */
@ConfigurationProperties(prefix = "squarewise.security.browser")
data class BrowserOriginProperties(
    /** Origins are scheme/host/port values; wildcards and paths are rejected. */
    var allowedOrigins: List<String> = emptyList()
)
