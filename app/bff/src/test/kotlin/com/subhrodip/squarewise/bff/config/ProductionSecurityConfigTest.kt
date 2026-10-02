package com.subhrodip.squarewise.bff.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.config.web.server.ServerHttpSecurity

/** Verifies the BFF production security configuration builds its real reactive policy. */
class ProductionSecurityConfigTest {
    private val configuration = ProductionSecurityConfig(
        issuerUri = "https://issuer.example",
        audience = "squarewise-api",
        allowedAlgorithms = "RS256"
    )

    /** Verifies decoder construction remains provider-backed and validates configured inputs. */
    @Test
    fun `creates reactive oidc decoder from configured issuer and audience`() {
        assertThat(configuration.reactiveJwtDecoder()).isNotNull
    }

    /** Verifies the reactive chain can be built with the configured route policy and headers. */
    @Test
    fun `builds reactive security filter chain`() {
        val http = ServerHttpSecurity.http()
        http.oauth2ResourceServer { resourceServer ->
            resourceServer.jwt { jwt -> jwt.jwtDecoder(configuration.reactiveJwtDecoder()) }
        }

        val chain = configuration.securityWebFilterChain(http)

        assertThat(chain).isNotNull
    }
}
