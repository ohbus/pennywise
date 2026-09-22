package com.subhrodip.pennywise.notifications.delivery.model

import java.time.Duration

/** Retry decision including bounded delay and parking state. */
data class RetryDecision(val retry: Boolean, val parked: Boolean, val delay: Duration)
