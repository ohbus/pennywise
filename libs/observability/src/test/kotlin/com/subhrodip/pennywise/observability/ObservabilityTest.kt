package com.subhrodip.pennywise.observability

import com.subhrodip.pennywise.observability.logging.LoggingContext
import com.subhrodip.pennywise.observability.security.SensitiveDataSanitizer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class ObservabilityTest {
    @Test
    fun `logging context restores and cleans MDC`() {
        MDC.put("requestId", "outer")
        try {
            assertEquals("value", LoggingContext.with("requestId", "value") { MDC.get("requestId") })
            assertEquals("outer", MDC.get("requestId"))
        } finally {
            MDC.clear()
        }
    }

    @Test
    fun `sanitizer masks valid and invalid personal values`() {
        assertEquals("u***@domain.com", SensitiveDataSanitizer.email("user@domain.com"))
        assertEquals("[REDACTED]", SensitiveDataSanitizer.email("not-an-email"))
        assertEquals("Bearer [REDACTED]", SensitiveDataSanitizer.token("Bearer secret"))
        assertEquals("[REDACTED]", SensitiveDataSanitizer.token("secret"))
    }
}
