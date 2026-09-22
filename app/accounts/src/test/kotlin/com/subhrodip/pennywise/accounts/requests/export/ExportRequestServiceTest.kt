package com.subhrodip.pennywise.accounts.requests.export

import com.subhrodip.pennywise.accounts.requests.export.model.ExportStatus
import com.subhrodip.pennywise.accounts.requests.export.service.ExportRequestService
import com.subhrodip.pennywise.accounts.requests.export.persistence.InMemoryExportRequestStore

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class ExportRequestServiceTest {
    @Test
    fun `creates export request with stable id and requested status`() {
        val request = ExportRequestService(InMemoryExportRequestStore { Instant.EPOCH }).request("alice")
        assertEquals("alice", request.subject)
        assertEquals(ExportStatus.REQUESTED, request.status)
    }
}
