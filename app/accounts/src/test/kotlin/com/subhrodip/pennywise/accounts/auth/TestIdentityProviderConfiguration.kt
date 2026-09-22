package com.subhrodip.pennywise.accounts.auth

import com.subhrodip.pennywise.accounts.auth.provider.IdentityProviderPort
import com.subhrodip.pennywise.accounts.auth.provider.InternalJwtTokenProvider
import java.util.Base64
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/** Explicit test-only identity provider; production uses the external OIDC adapter. */
@Configuration(proxyBeanMethods = false)
@Profile("test")
class TestIdentityProviderConfiguration {
    /** Creates the deterministic signer required by isolated authentication tests. */
    @Bean
    fun testIdentityProvider(
        @Value("\${PENNYWISE_SECURITY_JWT_SIGNING_SECRET}") encodedSecret: String,
        @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}") issuer: String,
        @Value("\${pennywise.security.oidc.audience}") audience: String
    ): IdentityProviderPort {
        val secret = Base64.getDecoder().decode(encodedSecret).also {
            require(it.size >= 32) { "Test JWT signing secret must contain at least 32 bytes" }
        }
        return InternalJwtTokenProvider(secret, issuer, audience)
    }
}
