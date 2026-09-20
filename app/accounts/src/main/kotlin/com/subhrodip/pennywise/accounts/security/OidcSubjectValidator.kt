package com.subhrodip.pennywise.accounts.security

import java.net.URI

/**
 * Validates OIDC issuer and subject format for incoming claims.
 *
 * @param expectedIssuer Trusted OIDC issuer URI.
 */
class OidcSubjectValidator(private val expectedIssuer: URI) {
    /**
     * Validates that the claims match the expected issuer and the subject conforms to identifier policy.
     *
     * @param claims Extracted OIDC claims.
     * @return Validated subject identifier string.
     */
    fun validate(claims: OidcClaims): String {
        require(URI.create(claims.issuer) == expectedIssuer) { "OIDC issuer is not trusted" }
        require(claims.subject.matches(Regex("^[A-Za-z0-9|._:-]{1,200}$"))) { "OIDC subject is invalid" }
        return claims.subject
    }
}
