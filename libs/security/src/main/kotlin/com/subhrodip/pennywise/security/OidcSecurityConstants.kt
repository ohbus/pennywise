package com.subhrodip.pennywise.security

/** Stable OIDC/JWT protocol names shared by validation adapters. */
object OidcSecurityConstants {
    /** JWT header containing the signing algorithm identifier. */
    const val ALGORITHM_HEADER: String = "alg"

    /** JWT subject claim used as the only durable provider identity. */
    const val SUBJECT_CLAIM: String = "sub"

    /** JWT audience claim used for API resource targeting. */
    const val AUDIENCE_CLAIM: String = "aud"

    /** Default asymmetric algorithm accepted by local and production policy. */
    const val DEFAULT_SIGNING_ALGORITHM: String = "RS256"

    /** OAuth error code for rejected bearer-token validation. */
    const val INVALID_TOKEN_ERROR_CODE: String = "invalid_token"

    /** Safe OAuth error description for policy-level token rejection. */
    const val ALGORITHM_REJECTED_DESCRIPTION: String = "JWT signing algorithm is not allowed"

    /** Safe configuration error when no issuer is configured. */
    const val ISSUER_REQUIRED_MESSAGE: String = "OIDC issuer URI is required"

    /** Safe configuration error when no audience is configured. */
    const val AUDIENCE_REQUIRED_MESSAGE: String = "OIDC audience is required"
}
