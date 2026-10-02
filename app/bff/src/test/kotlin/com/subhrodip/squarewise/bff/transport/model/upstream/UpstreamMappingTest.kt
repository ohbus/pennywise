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

        val upstream = UpstreamExpense(
            expenseId = "expense-1",
            version = 4,
            amount = amount,
            category = "TRAVEL",
            allocations = allocations
        )

        assertEquals("expense-1", upstream.expenseId)
        assertEquals(4, upstream.version)
        assertEquals(null, upstream.description)
        assertEquals(amount, upstream.amount)
        assertEquals("TRAVEL", upstream.category)
        assertEquals(allocations, upstream.allocations)

        val mapped = upstream.toBffExpense("Fallback description")

        assertEquals("expense-1", mapped.expenseId)
        assertEquals(4, mapped.version)
        assertEquals("Fallback description", mapped.description)
        assertEquals(amount, mapped.amount)
        assertEquals("TRAVEL", mapped.category)
        assertEquals(allocations, mapped.allocations)
    }

    @Test
    fun `settlement mapping preserves nullable participants and amount`() {
        val upstream = UpstreamSettlement(
            id = "settlement-1",
            fromParticipantId = null,
            toParticipantId = "bob",
            amountMinor = 900,
            currency = "EUR",
            status = "REVERSED"
        )

        assertEquals("settlement-1", upstream.id)
        assertEquals(null, upstream.fromParticipantId)
        assertEquals("bob", upstream.toParticipantId)
        assertEquals(900, upstream.amountMinor)
        assertEquals("EUR", upstream.currency)
        assertEquals("REVERSED", upstream.status)

        val mapped = upstream.toBffSettlement()

        assertEquals("settlement-1", mapped.id)
        assertEquals(null, mapped.from)
        assertEquals("bob", mapped.to)
        assertEquals(900, mapped.amountMinor)
        assertEquals("EUR", mapped.currency)
        assertEquals("REVERSED", mapped.status)

        val defaultSettlement = UpstreamSettlement(id = "settlement-2", currency = "USD")
        assertEquals(null, defaultSettlement.fromParticipantId)
        assertEquals(null, defaultSettlement.toParticipantId)
        assertEquals(null, defaultSettlement.amountMinor)
        assertEquals("RECORDED", defaultSettlement.status)
    }
}
