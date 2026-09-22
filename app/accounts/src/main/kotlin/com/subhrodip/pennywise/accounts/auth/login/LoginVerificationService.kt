package com.subhrodip.pennywise.accounts.auth.login

import com.subhrodip.pennywise.accounts.auth.credential.LoginCredentialService
import com.subhrodip.pennywise.accounts.auth.session.TokenResponse
import com.subhrodip.pennywise.accounts.auth.session.TokenSessionService
import com.subhrodip.pennywise.accounts.profile.persistence.ProfileStore
import com.subhrodip.pennywise.errors.domain.ApplicationException
import com.subhrodip.pennywise.errors.domain.ErrorCode
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional

/**
 * Service coordinating single-use credential redemption, profile resolution,
 * and authenticated session initialization.
 *
 * Implements the core passwordless verification flow:
 * 1. Atomically redeems the submitted credential (rejecting expired/replayed/unknown credentials).
 * 2. Resolves or provisions the user profile for the canonical subject (`internal:{email}`).
 * 3. Initializes a new refresh token family and mints access & refresh tokens via [TokenSessionService].
 *
 * @param credentialService Credential issuance and redemption port.
 * @param profileStore Profile lookup and provisioning port.
 * @param tokenSessionService Session family and token lifecycle coordinator.
 */
open class LoginVerificationService(
    private val credentialService: LoginCredentialService,
    private val profileStore: ProfileStore,
    private val tokenSessionService: TokenSessionService
) {
    private val log = LoggerFactory.getLogger(LoginVerificationService::class.java)

    /**
     * Verifies and redeems a one-time login link or code.
     *
     * @param credential Plaintext credential submitted by the user.
     * @param clientKind Client device/app type ("BROWSER" or "NATIVE").
     * @param deviceLabel Optional client or user-agent label.
     * @param now Current timestamp.
     * @return [TokenResponse] containing issued access and refresh tokens.
     * @throws ApplicationException with [ErrorCode.ERR_03] on invalid, expired, or replayed credentials.
     */
    @Transactional
    open fun verify(
        credential: String,
        clientKind: String,
        deviceLabel: String?,
        now: Instant
    ): TokenResponse {
        val redeemed = credentialService.redeem(credential, now)
            ?: throw ApplicationException(ErrorCode.ERR_03, "Authentication required")

        val canonicalEmail = redeemed.canonicalEmail
        val subject = "internal:$canonicalEmail"

        val profile = profileStore.get(subject)
        log.info("Successfully redeemed credential for accountId={}", profile.accountId)

        return tokenSessionService.createSession(
            accountId = profile.accountId,
            subject = subject,
            email = canonicalEmail,
            clientKind = clientKind,
            deviceLabel = deviceLabel,
            now = now
        )
    }
}
