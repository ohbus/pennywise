package com.subhrodip.squarewise.accounts.auth.jwks

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.JWKSelector
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import org.slf4j.LoggerFactory

/**
 * Default in-memory and configuration-backed implementation of [RsaKeyProvider].
 *
 * Supports loading standard PEM key pairs from properties, or automatically generates
 * a cryptographically secure 2048-bit RSA key pair at startup when none is provided.
 *
 * Supports thread-safe key rotation with overlapping public key verification.
 */
class DefaultRsaKeyProvider(
    private val properties: RsaKeyProperties
) : RsaKeyProvider {

    private val log = LoggerFactory.getLogger(DefaultRsaKeyProvider::class.java)
    private val activeKeyRef: AtomicReference<RSAKey>
    private val historicalKeys = CopyOnWriteArrayList<RSAKey>()

    init {
        val initialKey = loadOrGenerateKey()
        activeKeyRef = AtomicReference(initialKey)
        historicalKeys.add(initialKey.toPublicJWK())
        log.info("Initialized RSA key provider with active kid={}", initialKey.keyID)
    }

    override fun activeSigningKey(): RSAKey = activeKeyRef.get()

    override fun publicJwkSet(): JWKSet {
        val activePublic = activeKeyRef.get().toPublicJWK()
        val allPublic = (listOf(activePublic) + historicalKeys)
            .distinctBy { it.keyID }
        return JWKSet(allPublic)
    }

    override fun jwkSource(): JWKSource<SecurityContext> = JWKSource { jwkSelector: JWKSelector, _: SecurityContext? ->
        jwkSelector.select(publicJwkSet())
    }

    override fun rotateKey(newKey: RSAKey) {
        require(newKey.isPrivate) { "Rotated key must contain private key material for signing" }
        require(!newKey.keyID.isNullOrBlank()) { "Rotated key must have a non-blank kid" }

        val previous = activeKeyRef.getAndSet(newKey)
        historicalKeys.add(previous.toPublicJWK())
        historicalKeys.add(newKey.toPublicJWK())
        log.info("Rotated active RSA signing key from kid={} to kid={}", previous.keyID, newKey.keyID)
    }

    private fun loadOrGenerateKey(): RSAKey {
        val privPem = properties.privateKeyPem
        val pubPem = properties.publicKeyPem

        return if (!privPem.isNullOrBlank() && !pubPem.isNullOrBlank()) {
            parsePemKeyPair(properties.keyId, privPem, pubPem)
        } else {
            RSAKeyGenerator(2048)
                .keyID(properties.keyId)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .generate()
        }
    }

    private fun parsePemKeyPair(keyId: String, privatePem: String, publicPem: String): RSAKey {
        val keyFactory = KeyFactory.getInstance("RSA")

        val cleanPriv = privatePem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        val privBytes = Base64.getDecoder().decode(cleanPriv)
        val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privBytes)) as RSAPrivateKey

        val cleanPub = publicPem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("-----BEGIN RSA PUBLIC KEY-----", "")
            .replace("-----END RSA PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")
        val pubBytes = Base64.getDecoder().decode(cleanPub)
        val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(pubBytes)) as RSAPublicKey

        return RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(keyId)
            .keyUse(KeyUse.SIGNATURE)
            .algorithm(JWSAlgorithm.RS256)
            .build()
    }
}
