package com.subhrodip.pennywise.security

import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt

/** Rejects JWTs whose declared signing algorithm is outside deployment policy. */
class OidcJwtAlgorithmPolicy(
    allowedAlgorithms: Set<String>
) : OAuth2TokenValidator<Jwt> {
    private val allowed: Set<String> = allowedAlgorithms.map(String::trim).filter(String::isNotEmpty).toSet()

    init {
        require(allowed.isNotEmpty()) { "At least one OIDC signing algorithm is required" }
        require(allowed.none { it.startsWith("HS") }) { "Symmetric OIDC signing algorithms are not supported" }
    }

    override fun validate(token: Jwt): OAuth2TokenValidatorResult {
        val algorithm = token.headers[OidcSecurityConstants.ALGORITHM_HEADER] as? String
        return if (algorithm != null && algorithm in allowed) {
            OAuth2TokenValidatorResult.success()
        } else {
            OAuth2TokenValidatorResult.failure(
                OAuth2Error(
                    OidcSecurityConstants.INVALID_TOKEN_ERROR_CODE,
                    OidcSecurityConstants.ALGORITHM_REJECTED_DESCRIPTION,
                    null
                )
            )
        }
    }
}
