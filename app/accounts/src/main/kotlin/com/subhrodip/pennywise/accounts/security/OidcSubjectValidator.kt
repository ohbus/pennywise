package com.subhrodip.pennywise.accounts.security

import java.net.URI
import com.subhrodip.pennywise.security.OidcJwtClaimPolicy
import com.subhrodip.pennywise.security.OidcSecurityConstants

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
        require(URI.create(claims.issuer) == expectedIssuer) {
            OidcSecurityConstants.ISSUER_INVALID_DESCRIPTION
        }
        require(OidcJwtClaimPolicy.isValidSubject(claims.subject)) {
            OidcSecurityConstants.SUBJECT_INVALID_DESCRIPTION
        }
        return claims.subject
    }
}
