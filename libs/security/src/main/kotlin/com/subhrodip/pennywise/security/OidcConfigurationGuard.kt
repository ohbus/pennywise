package com.subhrodip.pennywise.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Fails a production-like application startup when its provider-neutral OIDC
 * resource-server settings are incomplete.
 *
 * The guard deliberately validates only deployment configuration. JWT
 * signature, issuer, audience, expiry, and algorithm validation remains the
 * responsibility of the Spring Security decoder configured by the application.
 * Local passthrough authentication is not covered by this component and is
 * available only under the explicit local profile.
 */
@Component
@Profile("production", "staging", "local-oidc")
class OidcConfigurationGuard(
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") private val issuerUri: String,
    @Value("\${pennywise.security.oidc.audience:}") private val audience: String,
    @Value("\${spring.profiles.active:}") private val activeProfiles: String
) {
    init {
        require(issuerUri.isNotBlank()) { OidcSecurityConstants.DEPLOYMENT_ISSUER_REQUIRED_MESSAGE }
        require(audience.isNotBlank()) { OidcSecurityConstants.DEPLOYMENT_AUDIENCE_REQUIRED_MESSAGE }
        require(issuerUri.startsWith("https://") || activeProfiles.split(',').contains("local-oidc")) {
            OidcSecurityConstants.DEPLOYMENT_ISSUER_HTTPS_MESSAGE
        }
    }
}
