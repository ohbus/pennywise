package com.subhrodip.pennywise.accounts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class ExportRequestServiceTest {
    @Test
    fun `creates export request with stable id and requested status`() {
        val request = ExportRequestService { Instant.EPOCH }.request("alice")
        assertEquals("alice", request.subject)
        assertEquals(ExportStatus.REQUESTED, request.status)
    }
}
