package com.subhrodip.pennywise.accounts.auth.credential

/** Port for one-time credential digests persisted by the authentication adapter. */
fun interface CredentialDigest {
    /**
     * Digests a raw credential for storage or constant-time comparison.
     *
     * @param credential raw value that must never be persisted or logged.
     * @return deterministic digest encoded for storage.
     */
    fun digest(credential: String): ByteArray
}
