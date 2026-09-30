package com.subhrodip.squarewise.accounts.auth.session

import java.time.Duration
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** Verifies fail-fast validation for every invalid session-policy dimension. */
class SessionPolicyBoundaryTest {
    @Test
    fun `rejects non-positive access-token lifetime`() {
        assertThrows(IllegalArgumentException::class.java) {
            SessionPolicy(Duration.ZERO, Duration.ofDays(30), Duration.ofDays(90))
        }
        assertThrows(IllegalArgumentException::class.java) {
            SessionPolicy(Duration.ofSeconds(-1), Duration.ofDays(30), Duration.ofDays(90))
        }
    }

    @Test
    fun `rejects non-positive refresh idle lifetime`() {
        assertThrows(IllegalArgumentException::class.java) {
            SessionPolicy(Duration.ofMinutes(10), Duration.ZERO, Duration.ofDays(90))
        }
        assertThrows(IllegalArgumentException::class.java) {
            SessionPolicy(Duration.ofMinutes(10), Duration.ofSeconds(-1), Duration.ofDays(90))
        }
    }

    @Test
    fun `rejects non-positive absolute lifetime`() {
        assertThrows(IllegalArgumentException::class.java) {
            SessionPolicy(Duration.ofMinutes(10), Duration.ofDays(30), Duration.ZERO)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SessionPolicy(Duration.ofMinutes(10), Duration.ofDays(30), Duration.ofSeconds(-1))
        }
    }

    @Test
    fun `rejects negative clock skew`() {
        assertThrows(IllegalArgumentException::class.java) {
            SessionPolicy(
                Duration.ofMinutes(10),
                Duration.ofDays(30),
                Duration.ofDays(90),
                Duration.ofSeconds(-1),
            )
        }
    }
}
