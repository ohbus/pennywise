package com.subhrodip.squarewise.accounts.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException

/** Verifies ordered local OIDC decoder fallback and fail-closed exhaustion. */
class FallbackJwtDecoderTest {
    @Test
    fun `requires at least one decoder`() {
        assertThrows(IllegalArgumentException::class.java) {
            FallbackJwtDecoder(emptyList())
        }
    }

    @Test
    fun `returns the first token accepted by the configured order`() {
        val first = mock(JwtDecoder::class.java)
        val second = mock(JwtDecoder::class.java)
        val expected = mock(Jwt::class.java)
        `when`(first.decode("token")).thenThrow(JwtException("first rejected"))
        `when`(second.decode("token")).thenReturn(expected)

        assertEquals(expected, FallbackJwtDecoder(listOf(first, second)).decode("token"))
    }

    @Test
    fun `returns immediately when the first decoder accepts`() {
        val first = mock(JwtDecoder::class.java)
        val second = mock(JwtDecoder::class.java)
        val expected = mock(Jwt::class.java)
        `when`(first.decode("token")).thenReturn(expected)

        assertEquals(expected, FallbackJwtDecoder(listOf(first, second)).decode("token"))
    }

    @Test
    fun `rethrows the final decoder validation failure`() {
        val firstFailure = JwtException("first rejected")
        val finalFailure = JwtException("final rejected")
        val first = mock(JwtDecoder::class.java)
        val second = mock(JwtDecoder::class.java)
        `when`(first.decode("token")).thenThrow(firstFailure)
        `when`(second.decode("token")).thenThrow(finalFailure)

        assertEquals(
            finalFailure,
            assertThrows(JwtException::class.java) {
                FallbackJwtDecoder(listOf(first, second)).decode("token")
            },
        )
    }
}
