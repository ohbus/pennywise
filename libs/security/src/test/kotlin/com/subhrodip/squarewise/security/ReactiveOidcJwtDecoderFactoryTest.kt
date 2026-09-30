package com.subhrodip.squarewise.security

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** Verifies reactive decoder construction fails before network discovery on incomplete config. */
class ReactiveOidcJwtDecoderFactoryTest {
    @Test
    fun `reactive decoder rejects blank issuer`() {
        assertThrows(IllegalArgumentException::class.java) {
            ReactiveOidcJwtDecoderFactory.create("", "squarewise-api")
        }
    }

    @Test
    fun `reactive decoder rejects blank audience`() {
        assertThrows(IllegalArgumentException::class.java) {
            ReactiveOidcJwtDecoderFactory.create("https://issuer.example", "")
        }
    }
}
