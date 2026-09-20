package com.subhrodip.pennywise.accounts.auth.provider

import java.util.UUID

/**
 * Adapter stub for federating or delegating token issuance to an external OIDC Identity Provider
 * (e.g., Keycloak, Auth0, Okta) via RFC 8693 OAuth 2.0 Token Exchange or direct delegation.
 *
 * Preserves the ability to switch between Pennywise internal identity and managed cloud IdPs
 * without modifying domain login verification or session rotation logic.
 *
 * @param externalIssuerUri Base URI of the external OIDC IdP.
 * @param clientId OAuth2 client identifier.
 * @param audience Expected token audience.
 */
class ExternalOidcTokenProvider(
    private val externalIssuerUri: String,
    private val clientId: String,
    private val audience: String
) : IdentityProviderPort {

    init {
        require(externalIssuerUri.isNotBlank()) { "External issuer URI must not be blank" }
        require(clientId.isNotBlank()) { "Client ID must not be blank" }
        require(audience.isNotBlank()) { "Audience must not be blank" }
    }

    /**
     * Issues an access token by coordinating with the external Identity Provider.
     *
     * @param accountId Stable account identifier UUID.
     * @param subject Canonical subject claim.
     * @param email Canonical normalized user email.
     * @return [IssuedToken] containing signed JWT string, scheme, and lifetime in seconds.
     */
    override fun issueAccessToken(accountId: UUID, subject: String, email: String): IssuedToken {
        // RFC 8693 Token Exchange / Client Credentials delegation hook
        throw UnsupportedOperationException("External OIDC provider delegation is not enabled in this profile")
    }
}
