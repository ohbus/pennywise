package com.subhrodip.squarewise.accounts.auth.jwks

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Unit tests for DefaultRsaKeyProvider verifying generation, JWKS export, and key rotation. */
class DefaultRsaKeyProviderTest {

    @Test
    fun `auto generates 2048-bit RSA key pair when no PEM is configured`() {
        val properties = RsaKeyProperties(keyId = "test-kid-1")
        val provider = DefaultRsaKeyProvider(properties)

        val activeKey = provider.activeSigningKey()
        assertNotNull(activeKey)
        assertTrue(activeKey.isPrivate)
        assertEquals("test-kid-1", activeKey.keyID)
        assertEquals(KeyUse.SIGNATURE, activeKey.keyUse)
        assertEquals(JWSAlgorithm.RS256, activeKey.algorithm)

        val jwkSet = provider.publicJwkSet()
        assertEquals(1, jwkSet.keys.size)
        assertFalse(jwkSet.keys[0].isPrivate)
        assertEquals("test-kid-1", jwkSet.keys[0].keyID)
    }

    @Test
    fun `supports key rotation retaining historical public keys in JWKSet`() {
        val properties = RsaKeyProperties(keyId = "initial-kid")
        val provider = DefaultRsaKeyProvider(properties)

        val initialJwkSet = provider.publicJwkSet()
        assertEquals(1, initialJwkSet.keys.size)
        assertEquals("initial-kid", initialJwkSet.keys[0].keyID)

        val newKey = RSAKeyGenerator(2048)
            .keyID("rotated-kid-2")
            .keyUse(KeyUse.SIGNATURE)
            .algorithm(JWSAlgorithm.RS256)
            .generate()

        provider.rotateKey(newKey)

        assertEquals("rotated-kid-2", provider.activeSigningKey().keyID)

        val rotatedJwkSet = provider.publicJwkSet()
        assertEquals(2, rotatedJwkSet.keys.size)
        val keyIds = rotatedJwkSet.keys.map { it.keyID }.toSet()
        assertTrue(keyIds.contains("initial-kid"))
        assertTrue(keyIds.contains("rotated-kid-2"))
    }
}
