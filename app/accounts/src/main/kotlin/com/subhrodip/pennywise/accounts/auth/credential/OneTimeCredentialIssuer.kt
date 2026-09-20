package com.subhrodip.pennywise.accounts.auth.credential

import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64

/** Issues high-entropy, expiring one-time credentials without retaining plaintext. */
class OneTimeCredentialIssuer(
    private val digest: CredentialDigest,
    private val random: SecureRandom = SecureRandom()
) {
    /** Digests a delivery-only credential using the configured storage policy. */
    fun digest(plaintext: String): ByteArray = digest.digest(plaintext)

    /**
     * Creates a credential envelope and its delivery-only plaintext.
     *
     * @param now issuance timestamp.
     * @param lifetime bounded validity duration.
     * @param maxAttempts maximum verification attempts before rejection.
     * @return plaintext for the delivery adapter plus digest-backed metadata.
     */
    fun issue(now: Instant, lifetime: Duration, maxAttempts: Int): IssuedCredential {
        require(!lifetime.isNegative && !lifetime.isZero) { "Credential lifetime must be positive" }
        require(lifetime <= MAXIMUM_LIFETIME) { "Credential lifetime exceeds policy" }
        require(maxAttempts in MINIMUM_ATTEMPTS..MAXIMUM_ATTEMPTS) { "Credential attempts are outside policy" }

        val bytes = ByteArray(RAW_BYTES).also(random::nextBytes)
        val plaintext = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        return IssuedCredential(
            plaintext = plaintext,
            digest = digest(plaintext),
            issuedAt = now,
            expiresAt = now.plus(lifetime),
            remainingAttempts = maxAttempts
        )
    }

    /** Digest-backed one-time credential metadata; plaintext is delivery-only. */
    data class IssuedCredential(
        val plaintext: String,
        val digest: ByteArray,
        val issuedAt: Instant,
        val expiresAt: Instant,
        val remainingAttempts: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as IssuedCredential

            if (plaintext != other.plaintext) return false
            if (!digest.contentEquals(other.digest)) return false
            if (issuedAt != other.issuedAt) return false
            if (expiresAt != other.expiresAt) return false
            if (remainingAttempts != other.remainingAttempts) return false

            return true
        }

        override fun hashCode(): Int {
            var result = plaintext.hashCode()
            result = 31 * result + digest.contentHashCode()
            result = 31 * result + issuedAt.hashCode()
            result = 31 * result + expiresAt.hashCode()
            result = 31 * result + remainingAttempts
            return result
        }

        override fun toString(): String =
            "IssuedCredential(plaintext='[REDACTED]', digest=${digest.contentToString()}, issuedAt=$issuedAt, expiresAt=$expiresAt, remainingAttempts=$remainingAttempts)"
    }

    private companion object {
        const val RAW_BYTES: Int = 32
        const val MINIMUM_ATTEMPTS: Int = 1
        const val MAXIMUM_ATTEMPTS: Int = 10
        val MAXIMUM_LIFETIME: Duration = Duration.ofMinutes(15)
    }
}
