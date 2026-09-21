package com.subhrodip.pennywise.expensecore.settlements

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertThrows
import com.subhrodip.pennywise.errors.ApplicationException

class SettlementServiceTest {
    @Test
    fun `records positive transfer and idempotently reverses`() {
        val service = SettlementService(InMemorySettlementStore())
        val groupId = UUID.randomUUID()
        val id = UUID.randomUUID()
        val settlement = service.record(groupId, id, UUID.randomUUID(), UUID.randomUUID(), 1250)
        assertEquals(SettlementStatus.RECORDED, settlement.status)
        assertEquals(SettlementStatus.REVERSED, service.reverse(groupId, id, "duplicate").status)
        assertEquals(SettlementStatus.REVERSED, service.reverse(groupId, id, "duplicate retry").status)
    }

    @Test
    fun `same durable settlement identity rejects a different payload`() {
        val service = SettlementService(InMemorySettlementStore())
        val groupId = UUID.randomUUID()
        val from = UUID.randomUUID()
        val to = UUID.randomUUID()
        service.record(groupId, UUID.randomUUID(), from, to, 1250, "actor-1", "settlement-key-0001")

        assertThrows(ApplicationException::class.java) {
            service.record(groupId, UUID.randomUUID(), from, to, 1300, "actor-1", "settlement-key-0001")
        }
    }
}
