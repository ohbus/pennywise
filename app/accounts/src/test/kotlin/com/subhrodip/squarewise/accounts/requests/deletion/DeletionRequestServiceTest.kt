package com.subhrodip.squarewise.accounts.requests.deletion

import com.subhrodip.squarewise.accounts.requests.deletion.model.DeletionStatus
import com.subhrodip.squarewise.accounts.requests.deletion.service.DeletionRequestService
import com.subhrodip.squarewise.accounts.requests.deletion.persistence.InMemoryDeletionRequestStore

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class DeletionRequestServiceTest {
    @Test
    fun `request is idempotent`() {
        val service = DeletionRequestService(InMemoryDeletionRequestStore { Instant.EPOCH })
        assertEquals(service.request("alice"), service.request("alice"))
        assertEquals(DeletionStatus.REQUESTED, service.get("alice")!!.status)
    }

    @Test
    fun `cancel and complete expose terminal store transitions`() {
        val service = DeletionRequestService(InMemoryDeletionRequestStore { Instant.EPOCH })

        service.request("cancelled")
        service.request("completed")

        assertEquals(DeletionStatus.CANCELLED, service.cancel("cancelled")?.status)
        assertEquals(DeletionStatus.COMPLETED, service.complete("completed")?.status)
    }
}
