package com.subhrodip.squarewise.accounts.auth.delivery.model

/** Context bound into the authenticated-encryption tag. */
data class CredentialDeliveryContext(
    val recipient: String,
    val template: AuthEmailTemplate
)
