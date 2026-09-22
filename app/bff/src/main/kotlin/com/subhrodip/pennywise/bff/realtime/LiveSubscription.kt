package com.subhrodip.pennywise.bff.realtime

import java.time.Instant

/** An authenticated, expiring group realtime subscription. */
data class LiveSubscription(val id: String, val userId: String, val groupId: String, val expiresAt: Instant)
