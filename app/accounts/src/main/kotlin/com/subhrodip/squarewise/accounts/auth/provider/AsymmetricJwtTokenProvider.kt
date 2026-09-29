package com.subhrodip.squarewise.accounts.auth.provider

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.subhrodip.squarewise.accounts.auth.jwks.RsaKeyProvider
import com.subhrodip.squarewise.ids.contracts.ApiEndpoints
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID

/**
 * Production-ready asymmetric JWT token provider signing access tokens with RSA RS256.
 *
 * Fulfills findings SEC-001 and SEC-009 by providing an operational, standards-compliant
 * token authority owned by Accounts. Tokens are verifiable across all downstream resource
 * servers via discoverable RFC 7517 JWKS endpoints without cross-service database lookups.
 *
 * @param rsaKeyProvider Key provider supplying the active RSA signing key and kid.
 * @param issuerUri Configured authoritative issuer identifier URI (`iss`).
 * @param audience Target API resource audience claim (`aud`).
 * @param tokenLifetime Validity duration for issued access tokens.
 */
class AsymmetricJwtTokenProvider(
    private val rsaKeyProvider: RsaKeyProvider,
    private val issuerUri: String,
    private val audience: String,
    private val tokenLifetime: Duration = Duration.ofMinutes(10)
) : IdentityProviderPort {

    init {
        require(issuerUri.isNotBlank()) { "Issuer URI must not be blank" }
        require(audience.isNotBlank()) { "Audience must not be blank" }
    }

    /**
     * Issues an RFC 7519 access token signed with RSA-SHA256 (RS256).
     *
     * @param accountId Stable account identifier UUID.
     * @param subject Canonical identity subject string.
     * @param email Canonical normalized user email.
     * @return [IssuedToken] containing signed JWT string, scheme, and lifetime in seconds.
     */
    override fun issueAccessToken(accountId: UUID, subject: String, email: String): IssuedToken {
        val now = Instant.now()
        val expiry = now.plus(tokenLifetime)
        val activeKey = rsaKeyProvider.activeSigningKey()

        val claimsSet = JWTClaimsSet.Builder()
            .issuer(issuerUri)
            .subject(subject)
            .audience(audience)
            .issueTime(Date.from(now))
            .notBeforeTime(Date.from(now))
            .expirationTime(Date.from(expiry))
            .claim("account_id", accountId.toString())
            .claim("email", email)
            .jwtID(UUID.randomUUID().toString())
            .build()

        val header = JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(activeKey.keyID)
            .type(JOSEObjectType.JWT)
            .build()

        val signedJwt = SignedJWT(header, claimsSet)
        val signer = RSASSASigner(activeKey.toRSAPrivateKey())
        signedJwt.sign(signer)

        return IssuedToken(
            accessToken = signedJwt.serialize(),
            tokenType = ApiEndpoints.Headers.BEARER_SCHEME,
            expiresIn = tokenLifetime.seconds
        )
    }
}
