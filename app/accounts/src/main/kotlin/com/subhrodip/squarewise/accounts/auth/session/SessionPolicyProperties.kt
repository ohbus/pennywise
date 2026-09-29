package com.subhrodip.squarewise.accounts.auth.session

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

/** Deployable authentication-session timing policy. */
@ConfigurationProperties(prefix = "squarewise.security.session")
data class SessionPolicyProperties(
    var accessTokenLifetime: Duration = Duration.ofMinutes(10),
    var refreshIdleLifetime: Duration = Duration.ofDays(30),
    var absoluteSessionLifetime: Duration = Duration.ofDays(90),
    var clockSkew: Duration = Duration.ofSeconds(30)
)
