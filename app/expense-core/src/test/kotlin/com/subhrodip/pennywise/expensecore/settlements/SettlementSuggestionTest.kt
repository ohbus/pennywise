package com.subhrodip.pennywise.expensecore.settlements

import com.subhrodip.pennywise.expensecore.expenses.ExpenseAllocation
import com.subhrodip.pennywise.expensecore.expenses.ExpensePayer
import com.subhrodip.pennywise.expensecore.expenses.ExpenseRecord
import com.subhrodip.pennywise.expensecore.expenses.GroupBalanceItem
import com.subhrodip.pennywise.expensecore.expenses.InMemoryExpenseStore
import com.subhrodip.pennywise.expensecore.expenses.MoneyDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class SettlementSuggestionTest {

    private val expenseStore = InMemoryExpenseStore()
    private val engine = SettlementSuggestionEngine(expenseStore)

    @Test
    fun `2-person split suggests single payment from debtor to creditor`() {
        val alice = UUID.randomUUID()
        val bob = UUID.randomUUID()

        // Alice paid 100 EUR, split equally: Alice net +50, Bob net -50
        val balances = listOf(
            GroupBalanceItem(alice.toString(), MoneyDto("EUR", "50")),
            GroupBalanceItem(bob.toString(), MoneyDto("EUR", "-50"))
        )

        val suggestions = engine.calculateSuggestions(balances)

        assertEquals(1, suggestions.size)
        val suggestion = suggestions[0]
        assertEquals(bob, suggestion.fromParticipantId)
        assertEquals(alice, suggestion.toParticipantId)
        assertEquals(50L, suggestion.amountMinor)
        assertEquals("EUR", suggestion.currency)
    }

    @Test
    fun `3-person cycle simplifies directly bypassing middle participant`() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val c = UUID.randomUUID()

        // A owes B 100 USD (A -100, B +100)
        // B owes C 100 USD (B -100, C +100)
        // Net: A = -100, B = 0, C = +100
        val balances = listOf(
            GroupBalanceItem(a.toString(), MoneyDto("USD", "-100")),
            GroupBalanceItem(b.toString(), MoneyDto("USD", "0")),
            GroupBalanceItem(c.toString(), MoneyDto("USD", "100"))
        )

        val suggestions = engine.calculateSuggestions(balances)

        // B should be completely bypassed; A pays C directly
        assertEquals(1, suggestions.size)
        val suggestion = suggestions[0]
        assertEquals(a, suggestion.fromParticipantId)
        assertEquals(c, suggestion.toParticipantId)
        assertEquals(100L, suggestion.amountMinor)
        assertEquals("USD", suggestion.currency)
    }

    @Test
    fun `multi-currency isolation keeps currencies separated`() {
        val alice = UUID.randomUUID()
        val bob = UUID.randomUUID()

        // In USD: Alice +1000, Bob -1000
        // In EUR: Alice -400, Bob +400
        val balances = listOf(
            GroupBalanceItem(alice.toString(), MoneyDto("USD", "1000")),
            GroupBalanceItem(bob.toString(), MoneyDto("USD", "-1000")),
            GroupBalanceItem(alice.toString(), MoneyDto("EUR", "-400")),
            GroupBalanceItem(bob.toString(), MoneyDto("EUR", "400"))
        )

        val suggestions = engine.calculateSuggestions(balances)

        assertEquals(2, suggestions.size)

        val eurSuggestion = suggestions.first { it.currency == "EUR" }
        assertEquals(alice, eurSuggestion.fromParticipantId)
        assertEquals(bob, eurSuggestion.toParticipantId)
        assertEquals(400L, eurSuggestion.amountMinor)

        val usdSuggestion = suggestions.first { it.currency == "USD" }
        assertEquals(bob, usdSuggestion.fromParticipantId)
        assertEquals(alice, usdSuggestion.toParticipantId)
        assertEquals(1000L, usdSuggestion.amountMinor)
    }

    @Test
    fun `zero balance handling produces no suggestions`() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()

        val balances = listOf(
            GroupBalanceItem(a.toString(), MoneyDto("EUR", "0")),
            GroupBalanceItem(b.toString(), MoneyDto("EUR", "0"))
        )

        val suggestions = engine.calculateSuggestions(balances)
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `empty balances produce empty suggestions`() {
        val suggestions = engine.calculateSuggestions(emptyList())
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `greedy largest debtor pays largest creditor matching`() {
        val d1 = UUID.randomUUID()
        val d2 = UUID.randomUUID()
        val c1 = UUID.randomUUID()
        val c2 = UUID.randomUUID()

        // Debtors: D1 owes 1000, D2 owes 500
        // Creditors: C1 is owed 800, C2 is owed 700
        val balances = listOf(
            GroupBalanceItem(d1.toString(), MoneyDto("USD", "-1000")),
            GroupBalanceItem(d2.toString(), MoneyDto("USD", "-500")),
            GroupBalanceItem(c1.toString(), MoneyDto("USD", "800")),
            GroupBalanceItem(c2.toString(), MoneyDto("USD", "700"))
        )

        val suggestions = engine.calculateSuggestions(balances)

        // Step 1: D1 (-1000) matched with C1 (+800) -> D1 pays C1 800 (D1 left: -200, C1 done)
        // Step 2: D2 (-500) is now largest debtor, matched with C2 (+700) -> D2 pays C2 500 (D2 done, C2 left: +200)
        // Step 3: D1 (-200) matched with C2 (+200) -> D1 pays C2 200 (both done)
        assertEquals(3, suggestions.size)

        assertEquals(SuggestedSettlement(d1, c1, 800L, "USD"), suggestions[0])
        assertEquals(SuggestedSettlement(d2, c2, 500L, "USD"), suggestions[1])
        assertEquals(SuggestedSettlement(d1, c2, 200L, "USD"), suggestions[2])
    }

    @Test
    fun `suggestSettlements integration with ExpenseStore`() {
        val groupId = UUID.randomUUID()
        val alice = UUID.randomUUID()
        val bob = UUID.randomUUID()

        // Create an expense in store: Alice paid 200 USD, split equally with Bob
        val record = ExpenseRecord(
            expenseId = UUID.randomUUID(),
            groupId = groupId,
            description = "Dinner",
            category = "food",
            currency = "USD",
            amountMinor = 200,
            version = 1,
            allocationMode = "EQUAL",
            createdAt = Instant.now(),
            payers = listOf(ExpensePayer(alice, 200)),
            allocations = listOf(ExpenseAllocation(alice, 100), ExpenseAllocation(bob, 100))
        )
        expenseStore.create(groupId, record, "idempotency-key-1")

        val suggestions = engine.suggestSettlements(groupId)

        assertEquals(1, suggestions.size)
        assertEquals(bob, suggestions[0].fromParticipantId)
        assertEquals(alice, suggestions[0].toParticipantId)
        assertEquals(100L, suggestions[0].amountMinor)
        assertEquals("USD", suggestions[0].currency)
    }
}
