package com.subhrodip.pennywise.accounts.auth.login

import com.subhrodip.pennywise.accounts.auth.abuse.LoginRateLimitService
import com.subhrodip.pennywise.accounts.auth.credential.LoginCredentialService
import com.subhrodip.pennywise.accounts.auth.delivery.AuthEmailMessage
import com.subhrodip.pennywise.accounts.auth.delivery.AuthEmailSender
import java.time.Instant

/** Coordinates the low-friction passwordless login-start use case. */
class LoginStartService(
    private val rateLimitService: LoginRateLimitService,
    private val credentialService: LoginCredentialService,
    private val emailSender: AuthEmailSender
) {
    /**
     * Starts login without exposing account existence, validation, throttling,
     * or delivery details to the caller.
     *
     * @param email raw user input; normalization occurs in the credential/key boundary.
     * @param networkPartition trusted server-derived abuse partition.
     * @param kind link or code delivery mode.
     * @param now request timestamp.
     * @return generic accepted response for every externally visible outcome.
     */
    fun start(
        email: String,
        networkPartition: String,
        kind: LoginCredentialService.CredentialKind,
        now: Instant
    ): LoginStartResult {
        val allowed = runCatching {
            rateLimitService.tryAcquire(email, networkPartition, now)
        }.getOrDefault(false)
        if (!allowed) return LoginStartResult.ACCEPTED

        val credential = runCatching {
            credentialService.issue(email, kind, now)
        }.getOrNull() ?: return LoginStartResult.ACCEPTED

        runCatching {
            emailSender.send(
                AuthEmailMessage(
                    recipient = credential.canonicalEmail,
                    template = kind.toTemplate(),
                    credential = credential.plaintext,
                    expiresAt = credential.expiresAt
                )
            )
        }
        return LoginStartResult.ACCEPTED
    }

    private fun LoginCredentialService.CredentialKind.toTemplate() =
        when (this) {
            LoginCredentialService.CredentialKind.LINK -> com.subhrodip.pennywise.accounts.auth.delivery.AuthEmailTemplate.LOGIN_LINK
            LoginCredentialService.CredentialKind.CODE -> com.subhrodip.pennywise.accounts.auth.delivery.AuthEmailTemplate.LOGIN_CODE
        }
}

/** Public login-start result intentionally contains no account or delivery state. */
enum class LoginStartResult { ACCEPTED }
