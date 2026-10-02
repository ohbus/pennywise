package com.subhrodip.squarewise.accounts.auth.config

import com.subhrodip.squarewise.accounts.auth.provider.ExternalOidcTokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Verifies Accounts exposes the explicit external OIDC provider adapter when configured. */
class AuthSessionConfigurationTest {
    /** Verifies provider wiring creates the provider-neutral adapter without a fallback authority. */
    @Test
    fun `creates external oidc provider from configured issuer and audience`() {
        val provider = AuthSessionConfiguration(
            issuerUri = "https://issuer.example",
            audience = "squarewise-api"
        ).externalIdentityProviderPort()

        assertThat(provider).isInstanceOf(ExternalOidcTokenProvider::class.java)
    }
}
