package com.subhrodip.pennywise.expensecore.settlements

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

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
}
