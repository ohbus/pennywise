package com.subhrodip.pennywise.security

/** Stable OIDC/JWT protocol names shared by validation adapters. */
object OidcSecurityConstants {
    /** JWT header containing the signing algorithm identifier. */
    const val ALGORITHM_HEADER: String = "alg"

    /** JWT subject claim used as the only durable provider identity. */
    const val SUBJECT_CLAIM: String = "sub"

    /** Safe rejection description for a missing or malformed subject claim. */
    const val SUBJECT_INVALID_DESCRIPTION: String = "OIDC subject is invalid"

    /** Safe rejection description for an issuer outside the configured trust boundary. */
    const val ISSUER_INVALID_DESCRIPTION: String = "OIDC issuer is not trusted"

    /** JWT audience claim used for API resource targeting. */
    const val AUDIENCE_CLAIM: String = "aud"

    /** Default asymmetric algorithm accepted by local and production policy. */
    const val DEFAULT_SIGNING_ALGORITHM: String = "RS256"

    /** Prefix identifying symmetric HMAC algorithms, which are disallowed. */
    const val SYMMETRIC_ALGORITHM_PREFIX: String = "HS"

    /** OAuth error code for rejected bearer-token validation. */
    const val INVALID_TOKEN_ERROR_CODE: String = "invalid_token"

    /** Safe OAuth error description for policy-level token rejection. */
    const val ALGORITHM_REJECTED_DESCRIPTION: String = "JWT signing algorithm is not allowed"

    /** Safe configuration error when no issuer is configured. */
    const val ISSUER_REQUIRED_MESSAGE: String = "OIDC issuer URI is required"

    /** Safe configuration error when no audience is configured. */
    const val AUDIENCE_REQUIRED_MESSAGE: String = "OIDC audience is required"

    /** Configuration error raised when no signing algorithm is allowed. */
    const val SIGNING_ALGORITHM_REQUIRED_MESSAGE: String = "At least one OIDC signing algorithm is required"

    /** Configuration error raised when symmetric signing is configured. */
    const val SYMMETRIC_SIGNING_UNSUPPORTED_MESSAGE: String =
        "Symmetric OIDC signing algorithms are not supported"

    /** Safe production/staging configuration error when the issuer is incomplete. */
    const val DEPLOYMENT_ISSUER_REQUIRED_MESSAGE: String =
        "OIDC issuer URI is required in production and staging"

    /** Safe production/staging configuration error when the audience is incomplete. */
    const val DEPLOYMENT_AUDIENCE_REQUIRED_MESSAGE: String =
        "OIDC audience is required in production and staging"

    /** Safe production/staging configuration error when the issuer is not HTTPS. */
    const val DEPLOYMENT_ISSUER_HTTPS_MESSAGE: String =
        "OIDC issuer URI must use HTTPS outside local development"
}
