package com.subhrodip.pennywise.accounts.auth.abuse

/** Indicates that a rate-limit decision could not be made and the request must fail closed. */
class RateLimitStoreUnavailableException(cause: Throwable) : RuntimeException("Rate-limit store unavailable", cause)
