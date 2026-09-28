package com.subhrodip.pennywise.accounts.auth.abuse

import java.time.Instant

/** State atomically stored per canonical email/network abuse key. */
data class LoginRateLimitState(val windowStartedAt: Instant, val requestCount: Int, val lastRequestedAt: Instant?)
