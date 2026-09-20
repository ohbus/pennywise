package com.subhrodip.pennywise.accounts.auth.delivery

/** Protects a one-time credential for a bounded delivery handoff. */
interface CredentialEnvelopeProtector {
    /**
     * Encrypts a plaintext credential and authenticates its delivery context.
     *
     * @param plaintext credential that must never be persisted in plaintext.
     * @param context canonical recipient and template context.
     * @return versioned, URL-safe encrypted envelope.
     */
    fun protect(plaintext: String, context: CredentialDeliveryContext): String

    /** Decrypts and authenticates a delivery envelope for the exact context. */
    fun reveal(envelope: String, context: CredentialDeliveryContext): String
}

/** Context bound into the authenticated-encryption tag. */
data class CredentialDeliveryContext(
    val recipient: String,
    val template: AuthEmailTemplate
)
