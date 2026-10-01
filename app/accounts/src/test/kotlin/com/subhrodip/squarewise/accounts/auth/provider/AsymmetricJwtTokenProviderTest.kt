package com.subhrodip.squarewise.accounts.auth.provider

import com.subhrodip.squarewise.accounts.auth.jwks.DefaultRsaKeyProvider
import com.subhrodip.squarewise.accounts.auth.jwks.RsaKeyProperties
import com.subhrodip.squarewise.security.OidcJwtDecoderFactory
import java.time.Duration
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** Unit tests for AsymmetricJwtTokenProvider verifying RS256 issuance and claim validity. */
class AsymmetricJwtTokenProviderTest {

    private val rsaKeyProvider = DefaultRsaKeyProvider(RsaKeyProperties(keyId = "sec01a-test-key"))
    private val issuerUri = "https://accounts.squarewise.test"
    private val audience = "squarewise-api"
    private val tokenLifetime = Duration.ofMinutes(15)

    private val provider = AsymmetricJwtTokenProvider(
        rsaKeyProvider = rsaKeyProvider,
        issuerUri = issuerUri,
        audience = audience,
        tokenLifetime = tokenLifetime
    )

    @Test
    fun `issues RS256 token decodable by OidcJwtDecoderFactory with all required claims`() {
        val accountId = UUID.randomUUID()
        val subject = "usr-test-subject-1"
        val email = "user@example.com"

        val issued = provider.issueAccessToken(accountId, subject, email)

        assertNotNull(issued.accessToken)
        assertEquals("Bearer", issued.tokenType)
        assertEquals(tokenLifetime.seconds, issued.expiresIn)

        // Verify with OidcJwtDecoderFactory using the public key from the key provider
        val decoder = OidcJwtDecoderFactory.createWithPublicKey(
            publicKey = rsaKeyProvider.activeSigningKey().toRSAPublicKey(),
            issuerUri = issuerUri,
            audience = audience
        )

        val jwt = decoder.decode(issued.accessToken)
        assertNotNull(jwt)
        assertEquals(issuerUri, jwt.issuer.toString())
        assertEquals(subject, jwt.subject)
        assertEquals(listOf(audience), jwt.audience)
        assertEquals(accountId.toString(), jwt.getClaimAsString("account_id"))
        assertEquals(email, jwt.getClaimAsString("email"))
        assertEquals("sec01a-test-key", jwt.headers["kid"])
        assertEquals("RS256", jwt.headers["alg"])
    }

    @Test
    fun `rejects a blank issuer before token issuance`() {
        assertThrows(IllegalArgumentException::class.java) {
            AsymmetricJwtTokenProvider(
                rsaKeyProvider = rsaKeyProvider,
                issuerUri = " ",
                audience = audience,
                tokenLifetime = tokenLifetime,
            )
        }
    }

    @Test
    fun `rejects a blank audience before token issuance`() {
        assertThrows(IllegalArgumentException::class.java) {
            AsymmetricJwtTokenProvider(
                rsaKeyProvider = rsaKeyProvider,
                issuerUri = issuerUri,
                audience = "\t",
                tokenLifetime = tokenLifetime,
            )
        }
    }
}
