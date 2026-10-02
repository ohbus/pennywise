package com.subhrodip.squarewise.bff.transport.model.upstream

import com.subhrodip.squarewise.bff.transport.model.output.BffAllocation
import com.subhrodip.squarewise.bff.transport.model.output.BffMoney
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Verifies that upstream REST shapes retain BFF defaults and financial values. */
class UpstreamMappingTest {
    @Test
    fun `expense mapping preserves fields and uses fallback description when absent`() {
        val amount = BffMoney("EUR", "1250")
        val allocations = listOf(BffAllocation("alice", BffMoney("EUR", "625")))

        val mapped = UpstreamExpense(
            expenseId = "expense-1",
            version = 4,
            amount = amount,
            category = "TRAVEL",
            allocations = allocations
        ).toBffExpense("Fallback description")

        assertEquals("expense-1", mapped.expenseId)
        assertEquals(4, mapped.version)
        assertEquals("Fallback description", mapped.description)
        assertEquals(amount, mapped.amount)
        assertEquals("TRAVEL", mapped.category)
        assertEquals(allocations, mapped.allocations)
    }

    @Test
    fun `settlement mapping preserves nullable participants and amount`() {
        val mapped = UpstreamSettlement(
            id = "settlement-1",
            fromParticipantId = null,
            toParticipantId = "bob",
            amountMinor = 900,
            currency = "EUR",
            status = "REVERSED"
        ).toBffSettlement()

        assertEquals("settlement-1", mapped.id)
        assertEquals(null, mapped.from)
        assertEquals("bob", mapped.to)
        assertEquals(900, mapped.amountMinor)
        assertEquals("EUR", mapped.currency)
        assertEquals("REVERSED", mapped.status)
    }
}
