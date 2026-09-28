package com.subhrodip.pennywise.accounts.auth.abuse

/** Generic rate-limit result that does not encode account existence. */
enum class LoginRateLimitDecision { ALLOW, DENY }
