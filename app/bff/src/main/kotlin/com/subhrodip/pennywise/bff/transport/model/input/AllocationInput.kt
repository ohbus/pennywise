package com.subhrodip.pennywise.bff.transport.model.input

/** GraphQL allocation strategy input. */
data class AllocationInput(val mode: String, val items: List<AllocationItemInput>)
