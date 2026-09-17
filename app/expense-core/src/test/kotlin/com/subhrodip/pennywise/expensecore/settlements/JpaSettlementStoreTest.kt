package com.subhrodip.pennywise.expensecore.settlements

import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class JpaSettlementStoreTest @Autowired constructor(
    private val store: JpaSettlementStore
) {
    @Test
    fun `persists and idempotently reverses a settlement in its group`() {
        val groupId = UUID.randomUUID()
        val settlement = Settlement(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            1_250
        )

        assertEquals(SettlementStatus.RECORDED, store.record(groupId, settlement).status)
        assertEquals(SettlementStatus.REVERSED, store.reverse(groupId, settlement.id, "duplicate").status)
        assertEquals(SettlementStatus.REVERSED, store.reverse(groupId, settlement.id, "retry").status)
    }

    @Test
    fun `does not expose a settlement through another group`() {
        val groupId = UUID.randomUUID()
        val settlement = Settlement(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            1_250
        )
        store.record(groupId, settlement)

        assertThrows(IllegalStateException::class.java) {
            store.reverse(UUID.randomUUID(), settlement.id, "wrong group")
        }
    }
}
