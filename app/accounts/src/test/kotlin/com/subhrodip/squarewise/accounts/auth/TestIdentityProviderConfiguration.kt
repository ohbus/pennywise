package com.subhrodip.squarewise.accounts.auth

import com.subhrodip.squarewise.accounts.auth.provider.IdentityProviderPort
import com.subhrodip.squarewise.accounts.auth.provider.InternalJwtTokenProvider
import com.subhrodip.squarewise.accounts.auth.session.SessionPolicy
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
        @Value("\${SQUAREWISE_SECURITY_JWT_SIGNING_SECRET}") encodedSecret: String,
        @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}") issuer: String,
        @Value("\${squarewise.security.oidc.audience}") audience: String,
        sessionPolicy: SessionPolicy
    ): IdentityProviderPort {
        val secret = Base64.getDecoder().decode(encodedSecret).also {
            require(it.size >= 32) { "Test JWT signing secret must contain at least 32 bytes" }
        }
        return InternalJwtTokenProvider(secret, issuer, audience, sessionPolicy.accessTokenLifetime)
    }
}
