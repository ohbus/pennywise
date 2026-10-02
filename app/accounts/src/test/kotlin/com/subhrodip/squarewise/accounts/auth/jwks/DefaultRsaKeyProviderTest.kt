package com.subhrodip.squarewise.accounts.auth.jwks

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWKMatcher
import com.nimbusds.jose.jwk.JWKSelector
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Base64

/**
 * Unit tests for [DefaultRsaKeyProvider] verifying generation, JWKS export, key rotation overlap
 * windows (SEC-010), and guard conditions on rotation inputs.
 */
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
    fun `loads configured PKCS8 private and X509 public PEM keys`() {
        val source = RSAKeyGenerator(2048)
            .keyID("source-kid")
            .keyUse(KeyUse.SIGNATURE)
            .algorithm(JWSAlgorithm.RS256)
            .generate()
        val provider = DefaultRsaKeyProvider(
            RsaKeyProperties(
                keyId = "configured-kid",
                privateKeyPem = pem("PRIVATE KEY", source.toRSAPrivateKey().encoded),
                publicKeyPem = pem("PUBLIC KEY", source.toRSAPublicKey().encoded),
            ),
        )

        val activeKey = provider.activeSigningKey()

        assertTrue(activeKey.isPrivate)
        assertEquals("configured-kid", activeKey.keyID)
        assertEquals(source.toRSAPublicKey().modulus, activeKey.toRSAPublicKey().modulus)
    }

    @Test
    fun `generates a key when PEM configuration is only partially supplied`() {
        val source = RSAKeyGenerator(2048).generate()

        val privateOnly = DefaultRsaKeyProvider(
            RsaKeyProperties(
                keyId = "private-only",
                privateKeyPem = pem("PRIVATE KEY", source.toRSAPrivateKey().encoded),
            ),
        )
        val publicOnly = DefaultRsaKeyProvider(
            RsaKeyProperties(
                keyId = "public-only",
                publicKeyPem = pem("PUBLIC KEY", source.toRSAPublicKey().encoded),
            ),
        )

        assertTrue(privateOnly.activeSigningKey().isPrivate)
        assertEquals("private-only", privateOnly.activeSigningKey().keyID)
        assertTrue(publicOnly.activeSigningKey().isPrivate)
        assertEquals("public-only", publicOnly.activeSigningKey().keyID)
    }

    @Test
    fun `generates a key when PEM configuration contains blank values`() {
        val source = RSAKeyGenerator(2048).generate()

        val blankPrivate = DefaultRsaKeyProvider(
            RsaKeyProperties(
                keyId = "blank-private",
                privateKeyPem = "   ",
                publicKeyPem = pem("PUBLIC KEY", source.toRSAPublicKey().encoded),
            ),
        )
        val blankPublic = DefaultRsaKeyProvider(
            RsaKeyProperties(
                keyId = "blank-public",
                privateKeyPem = pem("PRIVATE KEY", source.toRSAPrivateKey().encoded),
                publicKeyPem = "   ",
            ),
        )

        assertTrue(blankPrivate.activeSigningKey().isPrivate)
        assertEquals("blank-private", blankPrivate.activeSigningKey().keyID)
        assertTrue(blankPublic.activeSigningKey().isPrivate)
        assertEquals("blank-public", blankPublic.activeSigningKey().keyID)
    }

    @Test
    fun `rejects malformed complete PEM configuration`() {
        assertThrows<IllegalArgumentException> {
            DefaultRsaKeyProvider(
                RsaKeyProperties(
                    keyId = "malformed",
                    privateKeyPem = "not-a-private-key",
                    publicKeyPem = "not-a-public-key",
                ),
            )
        }
    }

    private fun pem(label: String, bytes: ByteArray): String =
        "-----BEGIN $label-----\n" +
            Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(bytes) +
            "\n-----END $label-----"

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

    /**
     * SEC-010: the old public key must remain in the JWKS immediately after rotation
     * so that tokens signed with the previous key can still be verified during the overlap window.
     * Resource servers must not see a JWKS gap between rotation and cache refresh.
     */
    @Test
    fun `old public key is available in JWKS immediately after rotation for overlap verification`() {
        val properties = RsaKeyProperties(keyId = "key-v1")
        val provider = DefaultRsaKeyProvider(properties)

        val keyV2 = RSAKeyGenerator(2048)
            .keyID("key-v2")
            .keyUse(KeyUse.SIGNATURE)
            .algorithm(JWSAlgorithm.RS256)
            .generate()
        provider.rotateKey(keyV2)

        assertEquals("key-v2", provider.activeSigningKey().keyID)

        val jwks = provider.publicJwkSet()
        val kidToKey = jwks.keys.associateBy { it.keyID }
        assertTrue(kidToKey.containsKey("key-v1"), "Old key v1 must remain in JWKS after rotation")
        assertTrue(kidToKey.containsKey("key-v2"), "New key v2 must appear in JWKS after rotation")
        assertFalse(kidToKey["key-v1"]!!.isPrivate, "Historical keys must only expose public material")
    }

    /**
     * SEC-010: multiple successive rotations must accumulate historical keys so that a token
     * signed with any generation can still be validated until its generation is explicitly retired.
     */
    @Test
    fun `multiple successive rotations accumulate all historical public keys`() {
        val properties = RsaKeyProperties(keyId = "key-gen-1")
        val provider = DefaultRsaKeyProvider(properties)

        val keyGen2 = RSAKeyGenerator(2048).keyID("key-gen-2").keyUse(KeyUse.SIGNATURE).algorithm(JWSAlgorithm.RS256).generate()
        val keyGen3 = RSAKeyGenerator(2048).keyID("key-gen-3").keyUse(KeyUse.SIGNATURE).algorithm(JWSAlgorithm.RS256).generate()

        provider.rotateKey(keyGen2)
        provider.rotateKey(keyGen3)

        assertEquals("key-gen-3", provider.activeSigningKey().keyID)

        val allKids = provider.publicJwkSet().keys.map { it.keyID }.toSet()
        assertTrue(allKids.contains("key-gen-1"), "Gen-1 key must be retained in JWKS")
        assertTrue(allKids.contains("key-gen-2"), "Gen-2 key must be retained in JWKS")
        assertTrue(allKids.contains("key-gen-3"), "Active gen-3 key must be present in JWKS")
    }

    /**
     * SEC-010: rotation must reject a public-only key since signing would fail at token issuance time.
     * Fail closed: the active key must not change when an invalid key is presented.
     */
    @Test
    fun `rotateKey rejects public-only key without private material`() {
        val properties = RsaKeyProperties(keyId = "original")
        val provider = DefaultRsaKeyProvider(properties)

        val publicOnlyKey = RSAKeyGenerator(2048)
            .keyID("public-only")
            .keyUse(KeyUse.SIGNATURE)
            .algorithm(JWSAlgorithm.RS256)
            .generate()
            .toPublicJWK()

        assertThrows<IllegalArgumentException> {
            provider.rotateKey(publicOnlyKey)
        }
        // Active key must remain unchanged
        assertEquals("original", provider.activeSigningKey().keyID)
    }

    /**
     * SEC-010: rotation must reject a key with a blank or missing kid to preserve
     * JWKS disambiguation and historical key deduplication by kid.
     */
    @Test
    fun `rotateKey rejects key with blank kid`() {
        val properties = RsaKeyProperties(keyId = "original-kid")
        val provider = DefaultRsaKeyProvider(properties)

        val noKidKey = RSAKeyGenerator(2048)
            .keyUse(KeyUse.SIGNATURE)
            .algorithm(JWSAlgorithm.RS256)
            .generate()

        assertThrows<IllegalArgumentException> {
            provider.rotateKey(noKidKey)
        }
        assertEquals("original-kid", provider.activeSigningKey().keyID)
    }

    @Test
    fun `rotateKey rejects key with explicitly blank kid`() {
        val provider = DefaultRsaKeyProvider(RsaKeyProperties(keyId = "original-kid"))
        val blankKidKey = RSAKeyGenerator(2048)
            .keyID("")
            .keyUse(KeyUse.SIGNATURE)
            .algorithm(JWSAlgorithm.RS256)
            .generate()

        assertThrows<IllegalArgumentException> { provider.rotateKey(blankKidKey) }
        assertEquals("original-kid", provider.activeSigningKey().keyID)
    }

    /**
     * SEC-010: the JWKSource facade backed by the provider must surface the same public keys
     * as publicJwkSet so that resource-server token verification and JWKS discovery are consistent.
     */
    @Test
    fun `jwkSource returns the same public keys as publicJwkSet`() {
        val properties = RsaKeyProperties(keyId = "source-test-kid")
        val provider = DefaultRsaKeyProvider(properties)

        val jwkSetKids = provider.publicJwkSet().keys.map { it.keyID }.toSet()
        val sourceKids = provider.jwkSource()
            .get(JWKSelector(JWKMatcher.Builder().build()), null)
            .map { it.keyID }.toSet()

        assertEquals(jwkSetKids, sourceKids)
    }
}
