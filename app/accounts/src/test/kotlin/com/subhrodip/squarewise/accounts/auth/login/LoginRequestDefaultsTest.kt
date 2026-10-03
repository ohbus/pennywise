package com.subhrodip.squarewise.accounts.auth.login

import com.subhrodip.squarewise.accounts.auth.provider.IssuedToken
import com.subhrodip.squarewise.ids.contracts.ApiEndpoints
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Verifies stable defaults on the Accounts login request and issued-token contracts. */
class LoginRequestDefaultsTest {
    @Test
    fun `login request defaults optional routing fields to null`() {
        val request = LoginStartRequest("alice@example.test")

        assertEquals(null, request.channel)
        assertEquals(null, request.clientKind)
    }

    @Test
    fun `issued token defaults to the bearer scheme`() {
        val token = IssuedToken("signed-token", expiresIn = 600)

        assertEquals(ApiEndpoints.Headers.BEARER_SCHEME, token.tokenType)
    }
}
