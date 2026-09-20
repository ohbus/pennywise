package com.subhrodip.pennywise.accounts.security

/**
 * Encapsulates raw OIDC token claims for validation.
 *
 * @property issuer Expected OIDC issuer string.
 * @property subject Identity subject claim.
 */
data class OidcClaims(val issuer: String, val subject: String)
