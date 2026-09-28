package com.subhrodip.pennywise.bff.transport.model.output

/** REST payload used by the BFF to create a group upstream. */
data class BffCreateGroup(val name: String, val kind: String, val currency: String)
