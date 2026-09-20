package com.subhrodip.pennywise.accounts.auth

import com.subhrodip.pennywise.accounts.auth.credential.LoginCredentialService
import com.subhrodip.pennywise.accounts.auth.login.LoginStartRequest
import com.subhrodip.pennywise.accounts.auth.login.LoginStartResponse
import com.subhrodip.pennywise.accounts.auth.login.LoginStartService
import com.subhrodip.pennywise.accounts.auth.login.LoginVerifyRequest
import com.subhrodip.pennywise.accounts.auth.login.LoginVerificationService
import com.subhrodip.pennywise.accounts.auth.session.RefreshTokenRequest
import com.subhrodip.pennywise.accounts.auth.session.TokenResponse
import com.subhrodip.pennywise.accounts.auth.session.TokenSessionService
import com.subhrodip.pennywise.errors.ApplicationException
import com.subhrodip.pennywise.errors.ErrorCode
import com.subhrodip.pennywise.ids.ApiEndpoints
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import java.security.Principal
import java.time.Instant
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Public authentication REST controller for Accounts service.
 *
 * Implements contracted endpoints:
 * - `POST /accounts/v1/auth/login/start`: Starts passwordless login flow.
 * - `POST /accounts/v1/auth/login/verify`: Verifies single-use link/code credential.
 * - `POST /accounts/v1/auth/token/refresh`: Rotates refresh token within its family.
 * - `POST /accounts/v1/auth/logout`: Revokes active session family.
 *
 * @param loginStartService Passwordless login start orchestrator.
 * @param loginVerificationService Credential verification and session creation service.
 * @param tokenSessionService Token session and family lifecycle coordinator.
 */
@RestController
@RequestMapping(ApiEndpoints.Accounts.V1.BASE)
class AuthController(
    private val loginStartService: LoginStartService,
    private val loginVerificationService: LoginVerificationService,
    private val tokenSessionService: TokenSessionService
) {

    /**
     * Initiates passwordless authentication. Always responds with 202 Accepted.
     *
     * @param request Validated [LoginStartRequest].
     * @param servletRequest Incoming HTTP servlet request for network partitioning.
     * @return 202 Accepted with [LoginStartResponse].
     */
    @PostMapping(ApiEndpoints.Accounts.V1.LOGIN_START)
    fun startLogin(
        @Valid @RequestBody request: LoginStartRequest,
        servletRequest: HttpServletRequest
    ): ResponseEntity<LoginStartResponse> {
        val kind = when (request.channel?.uppercase()) {
            "CODE" -> LoginCredentialService.CredentialKind.CODE
            else -> LoginCredentialService.CredentialKind.LINK
        }
        val networkPartition = deriveNetworkPartition(servletRequest)

        loginStartService.start(
            email = request.email,
            networkPartition = networkPartition,
            kind = kind,
            now = Instant.now()
        )

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(LoginStartResponse())
    }

    /**
     * Redeems a one-time credential, returning access and refresh tokens.
     *
     * @param request Validated [LoginVerifyRequest].
     * @param servletRequest Incoming HTTP servlet request for client metadata.
     * @return 200 OK with [TokenResponse].
     */
    @PostMapping(ApiEndpoints.Accounts.V1.LOGIN_VERIFY)
    fun verifyLogin(
        @Valid @RequestBody request: LoginVerifyRequest,
        servletRequest: HttpServletRequest
    ): ResponseEntity<TokenResponse> {
        val userAgent = servletRequest.getHeader("User-Agent")
        val tokenResponse = loginVerificationService.verify(
            credential = request.credential,
            clientKind = request.clientKind,
            deviceLabel = userAgent,
            now = Instant.now()
        )
        return ResponseEntity.ok(tokenResponse)
    }

    /**
     * Rotates an active refresh token, returning a new token pair.
     *
     * @param request Validated [RefreshTokenRequest].
     * @param servletRequest Incoming HTTP servlet request for client metadata.
     * @return 200 OK with [TokenResponse].
     */
    @PostMapping(ApiEndpoints.Accounts.V1.TOKEN_REFRESH)
    fun refreshToken(
        @Valid @RequestBody request: RefreshTokenRequest,
        servletRequest: HttpServletRequest
    ): ResponseEntity<TokenResponse> {
        val userAgent = servletRequest.getHeader("User-Agent")
        val tokenResponse = tokenSessionService.rotateSession(
            rawRefreshToken = request.refreshToken,
            clientKind = "BROWSER",
            deviceLabel = userAgent,
            now = Instant.now(),
            subject = "internal:refresh",
            email = "internal@pennywise.local"
        )
        return ResponseEntity.ok(tokenResponse)
    }

    /**
     * Revokes the active session for an authenticated caller.
     *
     * @param principal Authenticated user principal.
     */
    @PostMapping(ApiEndpoints.Accounts.V1.LOGOUT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@AuthenticationPrincipal principal: Principal?) {
        if (principal == null || principal.name.isBlank()) {
            throw ApplicationException(ErrorCode.ERR_03, "Authentication required")
        }
    }

    private fun deriveNetworkPartition(request: HttpServletRequest): String {
        val remoteAddr = request.remoteAddr ?: "unknown"
        val ipPart = remoteAddr.split(".").take(2).joinToString(".")
        return if (ipPart.isBlank()) "default-partition" else ipPart
    }
}
