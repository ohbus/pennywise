package com.subhrodip.pennywise.accounts.requests.deletion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class DeletionRequestServiceTest {
    @Test
    fun `request is idempotent`() {
        val service = DeletionRequestService { Instant.EPOCH }
        assertEquals(service.request("alice"), service.request("alice"))
        assertEquals(DeletionStatus.REQUESTED, service.get("alice")!!.status)
    }
}
