package com.subhrodip.pennywise.bff

import com.subhrodip.pennywise.bff.config.UpstreamConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class UpstreamConfigurationTest {
    @Test
    fun `rejects missing deployed downstream addresses`() {
        assertThrows<IllegalArgumentException> { UpstreamConfiguration("", "http://expense-core") }
        assertThrows<IllegalArgumentException> { UpstreamConfiguration("http://accounts", "") }
    }

    @Test
    fun `accepts explicit deployed downstream addresses`() {
        assertDoesNotThrow { UpstreamConfiguration("http://accounts", "http://expense-core") }
    }
}
