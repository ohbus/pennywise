package com.subhrodip.pennywise.bff.graphql

import com.subhrodip.pennywise.bff.*
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.stereotype.Controller
import org.springframework.security.core.annotation.AuthenticationPrincipal
import java.security.Principal
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

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
    fun groupChanged(@Argument groupId: String, @AuthenticationPrincipal(expression = "tokenValue") principal: Any?): Flux<GroupInvalidation> =
        gateway.getGroup(groupId, bearerToken(principal))
            .flatMapMany { liveFanout.invalidations().filter { it.groupId == groupId } }

    @QueryMapping
    fun groups(@AuthenticationPrincipal(expression = "tokenValue") principal: Any?): Mono<List<BffGroup>> = gateway.listGroups(bearerToken(principal))

    @QueryMapping
    fun group(@Argument id: String, @AuthenticationPrincipal(expression = "tokenValue") principal: Any?): Mono<BffGroup> =
        gateway.getGroup(id, bearerToken(principal))

    @QueryMapping
    fun settlementSuggestions(@Argument groupId: String, @AuthenticationPrincipal(expression = "tokenValue") principal: Any?): Mono<List<BffSuggestedSettlement>> =
        gateway.getSettlementSuggestions(groupId, bearerToken(principal))

    @MutationMapping
    fun createGroup(@Argument input: CreateGroupInput, @AuthenticationPrincipal(expression = "tokenValue") principal: Any?): Mono<BffGroup> =
        gateway.createGroup(BffCreateGroup(input.name, input.kind, input.currency), bearerToken(principal))

    @MutationMapping
    fun updateGroup(
        @Argument groupId: String,
        @Argument name: String,
        @AuthenticationPrincipal(expression = "tokenValue") principal: Any?
    ): Mono<BffGroup> =
        gateway.updateGroup(groupId, name, bearerToken(principal))
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
        @AuthenticationPrincipal(expression = "tokenValue") principal: Any?
    ): Mono<BffExpense> =
        gateway.createExpense(groupId, input, idempotencyKey, bearerToken(principal))
            .doOnSuccess { expense ->
                if (expense != null) {
                    emitInvalidation(groupId, expense.version)
                }
            }

    @MutationMapping
    fun recordRepayment(
        @Argument input: RepaymentInput,
        @AuthenticationPrincipal(expression = "tokenValue") principal: Any?
    ): Mono<BffSettlement> {
        val groupId = input.groupId
            ?: return Mono.error(IllegalArgumentException("groupId is required for recording a repayment"))
        return gateway.recordRepayment(groupId, input, bearerToken(principal))
            .doOnSuccess { settlement ->
                if (settlement != null) {
                    emitInvalidation(groupId, 1L)
                }
            }
    }
}
