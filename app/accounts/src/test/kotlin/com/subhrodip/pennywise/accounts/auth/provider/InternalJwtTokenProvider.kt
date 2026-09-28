package com.subhrodip.pennywise.accounts.auth.provider

import com.subhrodip.pennywise.ids.contracts.ApiEndpoints

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID

/**
 * Built-in internal JWT token provider signing access tokens with HMAC-SHA256.
 *
 * Provides a self-contained, standards-compliant OIDC token issuer for Pennywise
 * without requiring an external identity provider service.
 *
 * @param secretSigningKey Cryptographically secure byte secret of at least 32 bytes.
 * @param issuerUri Configured issuer identifier URI for token claims (`iss`).
 * @param audience Target API audience for token claims (`aud`).
 * @param tokenLifetime Validity duration for issued access tokens (defaults to 15 minutes).
 */
class InternalJwtTokenProvider(
    private val secretSigningKey: ByteArray,
    private val issuerUri: String,
    private val audience: String,
    private val tokenLifetime: Duration = Duration.ofMinutes(15)
) : IdentityProviderPort {

    init {
        require(secretSigningKey.size >= MINIMUM_SECRET_BYTES) {
            "Secret signing key must contain at least $MINIMUM_SECRET_BYTES bytes"
        }
        require(issuerUri.isNotBlank()) { "Issuer URI must not be blank" }
        require(audience.isNotBlank()) { "Audience must not be blank" }
    }

    /**
     * Issues an HMAC-SHA256 signed access token conforming to RFC 7519.
     *
     * @param accountId Stable account identifier UUID.
     * @param subject Canonical subject claim.
     * @param email Canonical normalized user email.
     * @return [IssuedToken] containing signed JWT string, scheme, and lifetime in seconds.
     */
    override fun issueAccessToken(accountId: UUID, subject: String, email: String): IssuedToken {
        val now = Instant.now()
        val expiry = now.plus(tokenLifetime)

        val claimsSet = JWTClaimsSet.Builder()
            .issuer(issuerUri)
            .subject(subject)
            .audience(audience)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(expiry))
            .claim("account_id", accountId.toString())
            .claim("email", email)
            .jwtID(UUID.randomUUID().toString())
            .build()

        val signedJwt = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.HS256).build(),
            claimsSet
        )
        val signer = MACSigner(secretSigningKey)
        signedJwt.sign(signer)

        return IssuedToken(
            accessToken = signedJwt.serialize(),
            tokenType = ApiEndpoints.Headers.BEARER_SCHEME,
            expiresIn = tokenLifetime.seconds
        )
    }

    private companion object {
        const val MINIMUM_SECRET_BYTES: Int = 32
    }
}
