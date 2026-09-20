package com.subhrodip.pennywise.accounts.auth.credential

import com.subhrodip.pennywise.accounts.auth.identity.EmailAddress
import java.time.Duration
import java.time.Instant
import java.util.UUID
import org.springframework.transaction.annotation.Transactional

/** Application service coordinating canonical email credentials without provider coupling. */
open class LoginCredentialService(
    private val repository: LoginCredentialRepository,
    private val issuer: OneTimeCredentialIssuer
) {
    /**
     * Issues a delivery-only credential and persists only its digest.
     *
     * @param email raw user-provided email, normalized before persistence.
     * @param kind link or code delivery mode.
     * @param now issuance timestamp supplied by the caller for deterministic tests.
     * @param lifetime bounded credential lifetime.
     * @param maxAttempts maximum redemption attempts.
     * @return delivery envelope; callers must hand plaintext only to an email adapter.
     */
    @Transactional
    fun issue(
        email: String,
        kind: CredentialKind,
        now: Instant,
        lifetime: Duration = DEFAULT_LIFETIME,
        maxAttempts: Int = DEFAULT_ATTEMPTS
    ): DeliveryCredential {
        val canonicalEmail = EmailAddress.parse(email).value
        val issued = issuer.issue(now, lifetime, maxAttempts)
        val credentialId = UUID.randomUUID()
        repository.save(
            LoginCredentialEntity(
                credentialId = credentialId,
                canonicalEmail = canonicalEmail,
                credentialDigest = issued.digest,
                credentialKind = kind.name,
                issuedAt = issued.issuedAt,
                expiresAt = issued.expiresAt,
                remainingAttempts = issued.remainingAttempts
            )
        )
        return DeliveryCredential(credentialId, canonicalEmail, issued.plaintext, issued.expiresAt, kind)
    }

    /**
     * Attempts one atomic redemption; replay, expiry, and races produce the
     * same non-success result and reveal no account or credential state.
     *
     * @param plaintext raw value received from the link/code form.
     * @param now verification timestamp.
     * @return generic verification outcome.
     */
    @Transactional
    fun verify(plaintext: String, now: Instant): VerificationOutcome {
        return if (redeem(plaintext, now) != null) {
            VerificationOutcome.ACCEPTED
        } else {
            VerificationOutcome.REJECTED
        }
    }

    /**
     * Atomically redeems an active credential and returns its entity upon success.
     * Replay, expired, or invalid credentials return null.
     *
     * @param plaintext raw credential submitted by client.
     * @param now verification timestamp.
     * @return redeemed entity or null if redemption did not succeed.
     */
    @Transactional
    fun redeem(plaintext: String, now: Instant): LoginCredentialEntity? {
        if (plaintext.isBlank()) return null
        val digest = issuer.digest(plaintext)
        val updated = repository.consumeIfActive(digest, now)
        return if (updated == 1) {
            repository.findByCredentialDigest(digest)
        } else {
            null
        }
    }

    /** Delivery-only plaintext envelope; never persist or log `plaintext`. */
    data class DeliveryCredential(
        val credentialId: UUID,
        val canonicalEmail: String,
        val plaintext: String,
        val expiresAt: Instant,
        val kind: CredentialKind
    )

    /** Generic verification result intentionally independent of account state. */
    enum class VerificationOutcome { ACCEPTED, REJECTED }

    /** Supported one-time delivery forms. */
    enum class CredentialKind { LINK, CODE }

    private companion object {
        val DEFAULT_LIFETIME: Duration = Duration.ofMinutes(10)
        const val DEFAULT_ATTEMPTS: Int = 5
    }
}
