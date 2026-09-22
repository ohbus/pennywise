package com.subhrodip.pennywise.expensecore.settlements
import com.subhrodip.pennywise.expensecore.settlements.domain.Settlement
import com.subhrodip.pennywise.expensecore.settlements.domain.SettlementStatus
import com.subhrodip.pennywise.expensecore.settlements.persistence.JpaSettlementStore
import com.subhrodip.pennywise.expensecore.expenses.persistence.repository.BalancePostingRepository
import com.subhrodip.pennywise.expensecore.groups.domain.GroupEntity
import com.subhrodip.pennywise.expensecore.groups.persistence.repository.GroupRepository

import com.subhrodip.pennywise.errors.domain.ApplicationException
import com.subhrodip.pennywise.errors.domain.ErrorCode
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
    private val store: JpaSettlementStore,
    private val groupRepository: GroupRepository,
    private val balancePostingRepository: BalancePostingRepository
) {
    @Test
    fun `persists and idempotently reverses a settlement in its group`() {
        val groupId = UUID.randomUUID()
        groupRepository.save(GroupEntity(groupId, "Test group", "HOUSEHOLD", "EUR"))
        val settlement = Settlement(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            1_250
        )

        assertEquals(SettlementStatus.RECORDED, store.record(groupId, settlement).status)
        val recordedPostings = balancePostingRepository.findBySettlementId(settlement.id)
        assertEquals(2, recordedPostings.size)
        assertEquals(0L, recordedPostings.sumOf { it.amountMinor })
        assertEquals(1_250L, recordedPostings.first { it.participantId == settlement.fromParticipantId }.amountMinor)
        assertEquals(-1_250L, recordedPostings.first { it.participantId == settlement.toParticipantId }.amountMinor)
        assertEquals(SettlementStatus.REVERSED, store.reverse(groupId, settlement.id, "duplicate").status)
        val allPostings = balancePostingRepository.findBySettlementId(settlement.id)
        assertEquals(4, allPostings.size)
        assertEquals(0L, allPostings.sumOf { it.amountMinor })
        assertEquals(SettlementStatus.REVERSED, store.reverse(groupId, settlement.id, "retry").status)
        assertEquals(4, balancePostingRepository.findBySettlementId(settlement.id).size)
    }

    @Test
    fun `does not expose a settlement through another group`() {
        val groupId = UUID.randomUUID()
        groupRepository.save(GroupEntity(groupId, "Test group", "HOUSEHOLD", "EUR"))
        val settlement = Settlement(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            1_250
        )
        store.record(groupId, settlement)

        val error = assertThrows(ApplicationException::class.java) {
            store.reverse(UUID.randomUUID(), settlement.id, "wrong group")
        }
        assertEquals(ErrorCode.ERR_05, error.errorCode)
    }
}
