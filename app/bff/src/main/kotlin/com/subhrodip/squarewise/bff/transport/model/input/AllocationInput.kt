package com.subhrodip.squarewise.bff.transport.model.input

/** GraphQL allocation strategy input. */
data class AllocationInput(val mode: String, val items: List<AllocationItemInput>)
