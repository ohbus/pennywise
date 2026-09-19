package com.subhrodip.pennywise.bff.graphql

import com.subhrodip.pennywise.bff.*
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.security.Principal

@Controller
class GroupGraphqlController(
    private val gateway: ExpenseCoreGateway,
    fanout: LiveUpdateFanout? = null
) {
    private val liveFanout = fanout ?: LiveUpdateFanout()

    fun emitInvalidation(groupId: String, revision: Long): GroupInvalidation =
        liveFanout.emitInvalidation(groupId, revision)

    fun emitInvalidation(groupId: String, revision: Long, changeId: String): GroupInvalidation =
        liveFanout.emitInvalidation(groupId, revision, changeId)

    @SubscriptionMapping
    fun groupChanged(@Argument groupId: String, principal: Principal?): Flux<GroupInvalidation> =
        gateway.getGroup(groupId, principal?.name)
            .flatMapMany { liveFanout.invalidations().filter { it.groupId == groupId } }

    @QueryMapping
    fun groups(principal: Principal?): Mono<List<BffGroup>> = gateway.listGroups(principal?.name)

    @QueryMapping
    fun group(@Argument id: String, principal: Principal?): Mono<BffGroup> =
        gateway.getGroup(id, principal?.name)

    @QueryMapping
    fun settlementSuggestions(@Argument groupId: String, principal: Principal?): Mono<List<BffSuggestedSettlement>> =
        gateway.getSettlementSuggestions(groupId, principal?.name)

    @MutationMapping
    fun createGroup(@Argument input: CreateGroupInput, principal: Principal?): Mono<BffGroup> =
        gateway.createGroup(BffCreateGroup(input.name, input.kind, input.currency), principal?.name)

    @MutationMapping
    fun updateGroup(
        @Argument groupId: String,
        @Argument name: String,
        principal: Principal?
    ): Mono<BffGroup> =
        gateway.updateGroup(groupId, name, principal?.name)
            .doOnSuccess { group ->
                if (group != null) {
                    emitInvalidation(groupId, group.revision)
                }
            }

    @MutationMapping
    fun createExpense(
        @Argument groupId: String,
        @Argument input: CreateExpenseInput,
        @Argument idempotencyKey: String,
        principal: Principal?
    ): Mono<BffExpense> =
        gateway.createExpense(groupId, input, idempotencyKey, principal?.name)
            .doOnSuccess { expense ->
                if (expense != null) {
                    emitInvalidation(groupId, expense.version)
                }
            }

    @MutationMapping
    fun recordRepayment(
        @Argument input: RepaymentInput,
        principal: Principal?
    ): Mono<BffSettlement> {
        val groupId = input.groupId
            ?: return Mono.error(IllegalArgumentException("groupId is required for recording a repayment"))
        return gateway.recordRepayment(groupId, input, principal?.name)
            .doOnSuccess { settlement ->
                if (settlement != null) {
                    emitInvalidation(groupId, 1L)
                }
            }
    }
}
