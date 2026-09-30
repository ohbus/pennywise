package com.subhrodip.squarewise.accounts.security

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import com.subhrodip.squarewise.accounts.auth.jwks.RsaKeyProvider
import org.springframework.security.oauth2.jwt.JwtDecoder

/** Verifies the local OIDC profile can activate the external-plus-local decoder chain. */
class ProductionSecurityConfigTest {
    @Test
    fun `enables external decoder fallback only for local oidc`() {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        val issuer = "http://127.0.0.1:${server.address.port}"
        val metadata = """
            {"issuer":"$issuer","jwks_uri":"$issuer/jwks"}
        """.trimIndent().toByteArray(StandardCharsets.UTF_8)
        server.createContext("/.well-known/openid-configuration") { exchange ->
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, metadata.size.toLong())
            exchange.responseBody.use { it.write(metadata) }
        }
        val publicJwk = RSAKeyGenerator(2048)
            .keyID("discovery-test")
            .algorithm(JWSAlgorithm.RS256)
            .generate()
            .toPublicJWK()
            .toJSONString()
        val jwks = "{\"keys\":[$publicJwk]}".toByteArray(StandardCharsets.UTF_8)
        server.createContext("/jwks") { exchange ->
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, jwks.size.toLong())
            exchange.responseBody.use { it.write(jwks) }
        }
        server.start()

        try {
            val environment = mock(Environment::class.java)
            `when`(environment.acceptsProfiles(any(Profiles::class.java))).thenReturn(true)
            val keyProvider = mock(RsaKeyProvider::class.java)
            val source = JWKSource<SecurityContext> { _, _ -> emptyList() }
            `when`(keyProvider.jwkSource()).thenReturn(source)

            val decoder: JwtDecoder = ProductionSecurityConfig(
                issuerUri = issuer,
                audience = "squarewise-api",
                allowedAlgorithms = "RS256",
                externalValidationEnabled = true,
                environment = environment,
                rsaKeyProvider = keyProvider,
            ).jwtDecoder()

            assertTrue(decoder is FallbackJwtDecoder)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `does not enable external validation outside local oidc`() {
        val environment = mock(Environment::class.java)
        `when`(environment.acceptsProfiles(any(Profiles::class.java))).thenReturn(false)
        val keyProvider = mock(RsaKeyProvider::class.java)
        val source = JWKSource<SecurityContext> { _, _ -> emptyList() }
        `when`(keyProvider.jwkSource()).thenReturn(source)

        val decoder = ProductionSecurityConfig(
            issuerUri = "https://issuer.example",
            audience = "squarewise-api",
            allowedAlgorithms = "RS256",
            externalValidationEnabled = true,
            environment = environment,
            rsaKeyProvider = keyProvider,
        ).jwtDecoder()

        assertTrue(decoder !is FallbackJwtDecoder)
    }
}
