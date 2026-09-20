package com.subhrodip.pennywise.accounts.auth.abuse

import com.subhrodip.pennywise.accounts.auth.credential.CredentialDigest
import com.subhrodip.pennywise.accounts.auth.identity.EmailAddress

/** Derives opaque, deployment-secret-backed keys for passwordless throttling. */
class LoginRateLimitKeyDeriver(
    private val digest: CredentialDigest
) {
    /**
     * Derives a versioned digest from canonical email and trusted network data.
     *
     * @param email raw input, canonicalized exactly once here.
     * @param networkPartition server-derived coarse partition, never a raw header.
     * @return opaque digest suitable for persistence.
     */
    fun derive(email: String, networkPartition: String): ByteArray {
        val canonicalEmail = EmailAddress.parse(email).value
        val partition = networkPartition.trim()
        require(partition.isNotEmpty() && partition.length <= MAX_PARTITION_LENGTH) {
            "Network partition is invalid"
        }
        require(partition.none { it.isWhitespace() || it.isISOControl() }) {
            "Network partition contains invalid characters"
        }
        return digest.digest("v1|$canonicalEmail|$partition")
    }

    private companion object {
        const val MAX_PARTITION_LENGTH: Int = 128
    }
}
