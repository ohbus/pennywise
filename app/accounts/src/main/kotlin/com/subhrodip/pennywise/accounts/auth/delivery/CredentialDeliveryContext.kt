package com.subhrodip.pennywise.accounts.auth.delivery

/** Context bound into the authenticated-encryption tag. */
data class CredentialDeliveryContext(
    val recipient: String,
    val template: AuthEmailTemplate
)
