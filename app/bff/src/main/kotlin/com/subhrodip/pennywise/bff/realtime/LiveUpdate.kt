package com.subhrodip.pennywise.bff.realtime

/** A group revision delivered to a live subscription. */
data class LiveUpdate(val groupId: String, val revision: Long)
