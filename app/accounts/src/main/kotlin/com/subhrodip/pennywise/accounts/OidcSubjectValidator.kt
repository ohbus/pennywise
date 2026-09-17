package com.subhrodip.pennywise.accounts

import java.net.URI

data class OidcClaims(val issuer: String, val subject: String)

class OidcSubjectValidator(private val expectedIssuer: URI) {
    fun validate(claims: OidcClaims): String {
        require(URI.create(claims.issuer) == expectedIssuer) { "OIDC issuer is not trusted" }
        require(claims.subject.matches(Regex("^[A-Za-z0-9|._:-]{1,200}$"))) { "OIDC subject is invalid" }
        return claims.subject
    }
}
