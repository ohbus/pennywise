package com.subhrodip.pennywise.bff.graphql

import com.subhrodip.pennywise.ids.contracts.ApiEndpoints
import java.security.Principal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken

private const val BEARER_SCHEME = "${ApiEndpoints.Headers.BEARER_SCHEME} "

/** Extracts the opaque bearer credential for forwarding to authenticated upstreams. */
internal fun bearerToken(authorization: String?): String? =
    authorization
        ?.takeIf { it.startsWith(BEARER_SCHEME, ignoreCase = true) }
        ?.substring(BEARER_SCHEME.length)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

/** Returns the signed token value carried by Spring Security's authenticated principal. */
internal fun bearerToken(principal: Any?): String? =
    (principal as? String)
        ?: (principal as? Jwt)?.tokenValue
        ?: (principal as? AbstractOAuth2TokenAuthenticationToken<*>)?.token?.tokenValue
        ?: (principal as? Principal)?.name
