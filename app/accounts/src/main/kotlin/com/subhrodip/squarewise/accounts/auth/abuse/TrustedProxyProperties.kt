package com.subhrodip.squarewise.accounts.auth.abuse

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configures the set of trusted upstream proxies for client address resolution.
 *
 * Only requests arriving from a listed trusted proxy will have their `X-Forwarded-For`
 * or `X-Real-IP` headers consulted when deriving the true client address.
 * Direct connections and connections from untrusted downstreams always use the raw
 * remote socket address, preventing header spoofing.
 *
 * Addresses may be specified as exact IPv4/IPv6 addresses or in CIDR notation (e.g.
 * `10.0.0.0/8`, `172.16.0.0/12`, `::1/128`).
 */
@ConfigurationProperties(prefix = "squarewise.security.abuse.trusted-proxies")
data class TrustedProxyProperties(
    /** Trusted proxy addresses or CIDR ranges. Empty means no forwarded headers are trusted. */
    var addresses: List<String> = emptyList()
)
