package com.subhrodip.pennywise.bff.transport.model.input

/** GraphQL input for group creation. */
data class CreateGroupInput(val name: String, val kind: String, val currency: String)
