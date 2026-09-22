package com.subhrodip.pennywise.bff.transport.model.output

/** Group member representation exposed by the BFF. */
data class BffMember(val membershipId: String, val subject: String? = null, val displayName: String? = null, val isPlaceholder: Boolean? = null)
