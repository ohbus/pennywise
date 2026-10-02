package com.subhrodip.squarewise.bff.graphql

import java.security.Principal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

/**
 * Verifies bearer extraction and subject normalization at the GraphQL boundary.
 *
 * These tests protect the invariant that only a non-blank bearer credential is
 * forwarded and that every supported Spring Security principal representation
 * produces the same trimmed, non-blank subject value.
 */
class BearerAuthorizationTest {
    @Test
    fun `extracts bearer credentials case insensitively and trims surrounding whitespace`() {
        assertEquals("signed-token", bearerToken("bEaReR   signed-token  "))
    }

    @Test
    fun `rejects missing malformed and blank authorization headers`() {
        listOf(null, "", "Basic signed-token", "Bearer", "Bearer   ").forEach { value ->
            assertNull(bearerToken(value), "Authorization value must be rejected: $value")
        }
    }

    @Test
    fun `extracts supported principal token representations`() {
        val jwt = Jwt.withTokenValue("jwt-token")
            .header("alg", "RS256")
            .subject("jwt-subject")
            .build()
        val authentication = JwtAuthenticationToken(jwt)
        val namedPrincipal = Principal { "principal-subject" }

        assertEquals("raw-token", bearerToken("raw-token" as Any?))
        assertEquals("jwt-token", bearerToken(jwt))
        assertEquals("jwt-token", bearerToken(authentication))
        assertEquals("principal-subject", bearerToken(namedPrincipal))
    }

    @Test
    fun `normalizes supported subjects and rejects blank or unknown principals`() {
        val jwt = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("  jwt-subject  ")
            .build()
        val authentication = JwtAuthenticationToken(jwt)

        assertEquals("jwt-subject", authenticatedSubject(jwt))
        assertEquals("jwt-subject", authenticatedSubject(authentication))
        assertEquals("named-subject", authenticatedSubject(Principal { "  named-subject " }))
        assertEquals("raw-subject", authenticatedSubject(" raw-subject "))
        assertNull(authenticatedSubject(Principal { "  " }))
        assertNull(authenticatedSubject(42))
        assertNull(authenticatedSubject(null))
    }
}
