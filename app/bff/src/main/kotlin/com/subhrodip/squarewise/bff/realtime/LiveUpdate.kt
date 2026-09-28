package com.subhrodip.squarewise.bff.realtime

/** A group revision delivered to a live subscription. */
data class LiveUpdate(val groupId: String, val revision: Long)
