package com.subhrodip.squarewise.accounts.auth.jwks

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for RSA cryptographic keys used to sign access tokens.
 *
 * @param keyId Key identifier (kid) published in JWKS and JWT headers.
 * @param privateKeyPem Optional PEM-encoded PKCS#8 RSA private key.
 * @param publicKeyPem Optional PEM-encoded X.509 RSA public key.
 */
@ConfigurationProperties(prefix = "squarewise.security.jwt.rsa")
data class RsaKeyProperties(
    var keyId: String = "squarewise-2026-key1",
    var privateKeyPem: String? = null,
    var publicKeyPem: String? = null
)
