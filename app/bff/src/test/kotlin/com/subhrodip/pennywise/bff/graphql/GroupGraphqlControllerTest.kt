package com.subhrodip.pennywise.bff.graphql

import com.subhrodip.pennywise.bff.transport.ExpenseCoreGateway
import com.subhrodip.pennywise.bff.transport.AccountsGateway
import com.subhrodip.pennywise.bff.messaging.model.BffEventEnvelope
import com.subhrodip.pennywise.bff.messaging.model.ConsumptionResult
import com.subhrodip.pennywise.bff.messaging.model.DuplicateConsumptionResult
import com.subhrodip.pennywise.bff.messaging.model.ProcessedConsumptionResult
import com.subhrodip.pennywise.bff.messaging.service.BffEventConsumer
import com.subhrodip.pennywise.bff.messaging.persistence.BffEventDeduplicator
import com.subhrodip.pennywise.bff.realtime.GroupInvalidation
import com.subhrodip.pennywise.bff.realtime.LiveUpdate
import com.subhrodip.pennywise.bff.realtime.LiveUpdateFanout
import com.subhrodip.pennywise.bff.transport.model.input.AllocationInput
import com.subhrodip.pennywise.bff.transport.model.input.AllocationItemInput
import com.subhrodip.pennywise.bff.transport.model.output.BffAllocation
import com.subhrodip.pennywise.bff.transport.model.output.BffBalance
import com.subhrodip.pennywise.bff.transport.model.output.BffCreateGroup
import com.subhrodip.pennywise.bff.transport.model.output.BffExpense
import com.subhrodip.pennywise.bff.transport.model.output.BffGroup
import com.subhrodip.pennywise.bff.transport.model.output.BffMember
import com.subhrodip.pennywise.bff.transport.model.output.BffMoney
import com.subhrodip.pennywise.bff.transport.model.output.BffSettlement
import com.subhrodip.pennywise.bff.transport.model.output.BffSuggestedSettlement
import com.subhrodip.pennywise.bff.transport.model.input.CreateExpenseInput
import com.subhrodip.pennywise.bff.transport.model.input.CreateGroupInput
import com.subhrodip.pennywise.bff.transport.model.input.MoneyInput
import com.subhrodip.pennywise.bff.transport.model.input.PayerInput
import com.subhrodip.pennywise.bff.transport.model.input.RepaymentInput

import com.subhrodip.pennywise.bff.transport.UpstreamServiceException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import reactor.core.publisher.Mono
import java.security.Principal
import java.util.UUID

class GroupGraphqlControllerTest {

    private val gateway = mock(ExpenseCoreGateway::class.java)
    private val controller = GroupGraphqlController(gateway, LiveUpdateFanout())
    private val principal = Principal { "alice" }

    @Test
    fun `resolves groups query`() {
        val groupId = UUID.randomUUID().toString()
        val expected = listOf(BffGroup(groupId = groupId, name = "Trip", kind = "TRIP", revision = 2))

        `when`(gateway.listGroups("alice")).thenReturn(Mono.just(expected))

        val result = controller.groups(principal).block()
        assertNotNull(result)
        assertEquals(1, result?.size)
        assertEquals(groupId, result?.get(0)?.id)
        assertEquals("Trip", result?.get(0)?.name)
    }

    @Test
    fun `propagates group list member resolution failure`() {
        val failure = UpstreamServiceException(403, "forbidden")
        `when`(gateway.listGroups("alice")).thenReturn(Mono.error(failure))

        val thrown = assertThrows(UpstreamServiceException::class.java) { controller.groups(principal).block() }
        assertEquals(403, thrown.status)
    }

    @Test
    fun `resolves group by id with balances and expenses`() {
        val groupId = UUID.randomUUID().toString()
        val expenseId = UUID.randomUUID().toString()
        val participantId = UUID.randomUUID().toString()

        val expected = BffGroup(
            groupId = groupId,
            name = "Alps Cabin",
            kind = "TRIP",
            revision = 5,
            balances = listOf(
                BffBalance(participantId = participantId, amount = BffMoney("EUR", "1500"))
            ),
            expenses = listOf(
                BffExpense(
                    expenseId = expenseId,
                    version = 1,
                    description = "Groceries",
                    amount = BffMoney("EUR", "3000"),
                    category = "food",
                    allocations = listOf(BffAllocation(participantId, BffMoney("EUR", "1500")))
                )
            ),
            members = listOf(
                BffMember(membershipId = "mem-1", subject = "alice")
            )
        )

        `when`(gateway.getGroup(groupId, "alice")).thenReturn(Mono.just(expected))

        val result = controller.group(groupId, principal).block()
        assertNotNull(result)
        assertEquals(groupId, result?.id)
        assertEquals("Alps Cabin", result?.name)
        assertEquals(1, result?.balances?.size)
        assertEquals("1500", result?.balances?.get(0)?.money?.minor)
        assertEquals(1, result?.expenses?.size)
        assertEquals("Groceries", result?.expenses?.get(0)?.description)
        assertEquals(expenseId, result?.expenses?.get(0)?.id)
        assertEquals(1, result?.members?.size)
        assertEquals("mem-1", result?.members?.get(0)?.membershipId)
        assertEquals("alice", result?.members?.get(0)?.subject)
    }

    @Test
    fun `resolves settlementSuggestions query`() {
        val groupId = UUID.randomUUID().toString()
        val fromId = UUID.randomUUID().toString()
        val toId = UUID.randomUUID().toString()
        val expected = listOf(
            BffSuggestedSettlement(
                fromParticipantId = fromId,
                toParticipantId = toId,
                amountMinor = 1500,
                currency = "EUR"
            )
        )

        `when`(gateway.getSettlementSuggestions(groupId, "alice")).thenReturn(Mono.just(expected))

        val result = controller.settlementSuggestions(groupId, principal).block()
        assertNotNull(result)
        assertEquals(1, result?.size)
        assertEquals(fromId, result?.get(0)?.fromParticipantId)
        assertEquals(toId, result?.get(0)?.toParticipantId)
        assertEquals("1500", result?.get(0)?.amount?.minor)
        assertEquals("EUR", result?.get(0)?.amount?.currency)
    }

    @Test
    fun `resolves createGroup mutation`() {
        val groupId = UUID.randomUUID().toString()
        val input = CreateGroupInput("Household", "HOUSEHOLD", "EUR")
        val expected = BffGroup(groupId = groupId, name = "Household", kind = "HOUSEHOLD", revision = 1)

        `when`(gateway.createGroup(BffCreateGroup("Household", "HOUSEHOLD", "EUR"), "alice"))
            .thenReturn(Mono.just(expected))

        val result = controller.createGroup(input, principal).block()
        assertNotNull(result)
        assertEquals(groupId, result?.id)
        assertEquals("HOUSEHOLD", result?.kind)
    }

    @Test
    fun `resolves createExpense mutation`() {
        val groupId = UUID.randomUUID().toString()
        val expenseId = UUID.randomUUID().toString()
        val participantId = UUID.randomUUID().toString()
        val input = CreateExpenseInput(
            expenseId = expenseId,
            description = "Dinner",
            amount = MoneyInput("EUR", "2000"),
            payers = listOf(PayerInput(participantId, MoneyInput("EUR", "2000"))),
            allocation = AllocationInput("EQUAL", listOf(AllocationItemInput(participantId, "1")))
        )
        val expected = BffExpense(
            expenseId = expenseId,
            version = 1,
            description = "Dinner",
            amount = BffMoney("EUR", "2000"),
            category = "food",
            allocations = listOf(BffAllocation(participantId, BffMoney("EUR", "2000")))
        )

        `when`(gateway.createExpense(groupId, input, "idemp-exp-1", "alice"))
            .thenReturn(Mono.just(expected))

        val result = controller.createExpense(groupId, input, "idemp-exp-1", principal).block()
        assertNotNull(result)
        assertEquals(expenseId, result?.id)
        assertEquals("2000", result?.amount?.minor)
    }

    @Test
    fun `resolves recordRepayment mutation`() {
        val groupId = UUID.randomUUID().toString()
        val settlementId = UUID.randomUUID().toString()
        val fromId = UUID.randomUUID().toString()
        val toId = UUID.randomUUID().toString()
        val input = RepaymentInput(
            groupId = groupId,
            fromParticipantId = fromId,
            toParticipantId = toId,
            amount = MoneyInput("EUR", "1000"),
            reason = "Settling up lunch"
        )
        val expected = BffSettlement(
            id = settlementId,
            from = fromId,
            to = toId,
            amountMinor = 1000,
            status = "CONFIRMED",
            currency = "EUR"
        )

        `when`(gateway.recordRepayment(groupId, input, "alice"))
            .thenReturn(Mono.just(expected))

        val result = controller.recordRepayment(input, principal).block()
        assertNotNull(result)
        assertEquals(settlementId, result?.id)
        assertEquals("CONFIRMED", result?.status)
        assertEquals("1000", result?.amount?.minor)
        assertEquals("EUR", result?.amount?.currency)
    }

    @Test
    fun `recordRepayment rejects input without groupId`() {
        val input = RepaymentInput(
            groupId = null,
            fromParticipantId = UUID.randomUUID().toString(),
            toParticipantId = UUID.randomUUID().toString(),
            amount = MoneyInput("EUR", "1000"),
            reason = null
        )

        val mono = controller.recordRepayment(input, principal)
        assertThrows(IllegalArgumentException::class.java) {
            mono.block()
        }
    }

    @Test
    fun `groupChanged streams invalidation events for matching groupId and filters others`() {
        val targetGroupId = UUID.randomUUID().toString()
        val otherGroupId = UUID.randomUUID().toString()
        val events = mutableListOf<GroupInvalidation>()
        `when`(gateway.getGroup(targetGroupId, "alice")).thenReturn(Mono.just(BffGroup(targetGroupId, "Trip", "TRIP", "1")))

        val disposable = controller.groupChanged(targetGroupId, principal).subscribe { events.add(it) }
        try {
            controller.emitInvalidation(otherGroupId, 1L)
            controller.emitInvalidation(targetGroupId, 2L)
            controller.emitInvalidation(otherGroupId, 3L)
            controller.emitInvalidation(targetGroupId, 4L)

            assertEquals(2, events.size)
            assertEquals(targetGroupId, events[0].groupId)
            assertEquals(2L, events[0].revision)
            assertNotNull(events[0].changeId)
            assertEquals(targetGroupId, events[1].groupId)
            assertEquals(4L, events[1].revision)
            assertNotNull(events[1].changeId)
        } finally {
            disposable.dispose()
        }
    }

    @Test
    fun `createExpense mutation emits invalidation on success`() {
        val groupId = UUID.randomUUID().toString()
        val expenseId = UUID.randomUUID().toString()
        val participantId = UUID.randomUUID().toString()
        val input = CreateExpenseInput(
            expenseId = expenseId,
            description = "Dinner",
            amount = MoneyInput("EUR", "2000"),
            payers = listOf(PayerInput(participantId, MoneyInput("EUR", "2000"))),
            allocation = AllocationInput("EQUAL", listOf(AllocationItemInput(participantId, "1")))
        )
        val expected = BffExpense(
            expenseId = expenseId,
            version = 3,
            description = "Dinner",
            amount = BffMoney("EUR", "2000"),
            category = "food",
            allocations = listOf(BffAllocation(participantId, BffMoney("EUR", "2000")))
        )

        `when`(gateway.getGroup(groupId, "alice")).thenReturn(Mono.just(BffGroup(groupId, "Trip", "TRIP", "1")))

        `when`(gateway.createExpense(groupId, input, "idemp-exp-2", "alice"))
            .thenReturn(Mono.just(expected))

        val events = mutableListOf<GroupInvalidation>()
        val disposable = controller.groupChanged(groupId, principal).subscribe { events.add(it) }
        try {
            val result = controller.createExpense(groupId, input, "idemp-exp-2", principal).block()
            assertNotNull(result)
            assertEquals(1, events.size)
            assertEquals(groupId, events[0].groupId)
            assertEquals(3L, events[0].revision)
            assertNotNull(events[0].changeId)
        } finally {
            disposable.dispose()
        }
    }

    @Test
    fun `recordRepayment mutation emits invalidation on success`() {
        val groupId = UUID.randomUUID().toString()
        val settlementId = UUID.randomUUID().toString()
        val fromId = UUID.randomUUID().toString()
        val toId = UUID.randomUUID().toString()
        val input = RepaymentInput(
            groupId = groupId,
            fromParticipantId = fromId,
            toParticipantId = toId,
            amount = MoneyInput("EUR", "1000"),
            reason = "Settling up lunch"
        )
        val expected = BffSettlement(
            id = settlementId,
            from = fromId,
            to = toId,
            amountMinor = 1000,
            status = "CONFIRMED",
            currency = "EUR"
        )

        `when`(gateway.getGroup(groupId, "alice")).thenReturn(Mono.just(BffGroup(groupId, "Trip", "TRIP", "1")))

        `when`(gateway.recordRepayment(groupId, input, "alice"))
            .thenReturn(Mono.just(expected))

        val events = mutableListOf<GroupInvalidation>()
        val disposable = controller.groupChanged(groupId, principal).subscribe { events.add(it) }
        try {
            val result = controller.recordRepayment(input, principal).block()
            assertNotNull(result)
            assertEquals(1, events.size)
            assertEquals(groupId, events[0].groupId)
            assertEquals(1L, events[0].revision)
            assertNotNull(events[0].changeId)
        } finally {
            disposable.dispose()
        }
    }
}
