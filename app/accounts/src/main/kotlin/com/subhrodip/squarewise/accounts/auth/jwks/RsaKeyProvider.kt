package com.subhrodip.squarewise.accounts.auth.jwks

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext

/**
 * Port managing asymmetric RSA cryptographic key material for token signing,
 * rotation, and RFC 7517 JWKS discovery.
 */
interface RsaKeyProvider {
    /**
     * Returns the active RSA key used for signing new JWT tokens.
     * Contains the private key.
     */
    fun activeSigningKey(): RSAKey

    /**
     * Returns the RFC 7517 JWKSet containing all current and retiring public keys.
     * Strips all private key material.
     */
    fun publicJwkSet(): JWKSet

    /**
     * Returns a Nimbus JWKSource resolving public keys for local JWT decoders.
     */
    fun jwkSource(): JWKSource<SecurityContext>

    /**
     * Rotates the active signing key to a new RSA key, preserving previous public
     * keys in the JWKSet for token validation overlap.
     *
     * @param newKey Newly activated RSA key pair with private material.
     */
    fun rotateKey(newKey: RSAKey)
}
