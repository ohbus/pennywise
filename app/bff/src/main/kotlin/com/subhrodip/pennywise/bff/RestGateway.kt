package com.subhrodip.pennywise.bff

import com.subhrodip.pennywise.ids.ApiEndpoints
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.core.publisher.Flux
import java.time.Duration

data class BffMoney(val currency: String, val minor: String)

data class BffBalance(val participantId: String, val amount: BffMoney) {
    val money: BffMoney get() = amount
}

data class BffAllocation(val participantId: String, val amount: BffMoney)

data class BffExpense(
    val expenseId: String,
    val version: Long = 1,
    val description: String = "",
    val amount: BffMoney,
    val category: String? = null,
    val allocations: List<BffAllocation> = emptyList()
) {
    val id: String get() = expenseId
}

data class BffBalancesResponse(
    val groupId: String,
    val balances: List<BffBalance> = emptyList()
)

data class BffMember(val membershipId: String, val subject: String)

data class BffGroup(
    val groupId: String,
    val name: String,
    val kind: String = "TRIP",
    val revision: Long = 0,
    val balances: List<BffBalance> = emptyList(),
    val expenses: List<BffExpense> = emptyList(),
    val members: List<BffMember> = emptyList()
) {
    val id: String get() = groupId
}

data class BffCreateGroup(val name: String, val kind: String, val currency: String)

data class BffProfile(
    val accountId: String,
    val displayName: String,
    val timezone: String,
    val defaultCurrency: String
)

data class BffSettlement(
    val id: String,
    val from: String? = null,
    val to: String? = null,
    val amountMinor: Long? = null,
    val status: String,
    val currency: String = "EUR"
) {
    val amount: BffMoney get() = BffMoney(currency, (amountMinor ?: 0L).toString())
}

data class BffSuggestedSettlement(
    val fromParticipantId: String,
    val toParticipantId: String,
    val amountMinor: Long,
    val currency: String
) {
    val amount: BffMoney get() = BffMoney(currency, amountMinor.toString())
}

data class CreateGroupInput(val name: String, val kind: String, val currency: String)
data class MoneyInput(val currency: String, val minor: String)
data class PayerInput(val participantId: String, val amount: MoneyInput)
data class AllocationItemInput(val participantId: String, val value: String)
data class AllocationInput(val mode: String, val items: List<AllocationItemInput>)
data class CreateExpenseInput(
    val expenseId: String,
    val description: String,
    val amount: MoneyInput,
    val payers: List<PayerInput>,
    val allocation: AllocationInput
)
data class RepaymentInput(
    val groupId: String?,
    val fromParticipantId: String,
    val toParticipantId: String,
    val amount: MoneyInput,
    val reason: String?
)

class UpstreamServiceException(val status: Int, message: String = "Upstream service returned HTTP $status") : RuntimeException(message)

@Component
class AccountsGateway(
    builder: WebClient.Builder,
    @Value("${'$'}{pennywise.accounts-url:http://localhost:8081}") baseUrl: String,
    @Value("${'$'}{pennywise.bff.upstream-timeout:2s}") private val timeout: Duration
) {
    private val client = builder.baseUrl(baseUrl).build()

    fun getMe(bearer: String?): Mono<BffProfile> =
        client.get().uri(ApiEndpoints.Accounts.V1.PATH_ME)
            .headers { headers -> bearer?.let { headers.setBearerAuth(it) } }
            .retrieve()
            .onStatus({ it.isError }) { response -> Mono.error(UpstreamServiceException(response.statusCode().value(), "Accounts returned HTTP ${response.statusCode().value()}")) }
            .bodyToMono(BffProfile::class.java)
            .timeout(timeout)
}

@Component
class ExpenseCoreGateway(
    builder: WebClient.Builder,
    @Value("${'$'}{pennywise.expense-core-url:http://localhost:8082}") baseUrl: String,
    @Value("${'$'}{pennywise.bff.upstream-timeout:2s}") private val timeout: Duration
) {
    private val client = builder.baseUrl(baseUrl).build()

    fun createGroup(input: BffCreateGroup, bearer: String?): Mono<BffGroup> =
        client.post().uri(ApiEndpoints.ExpenseCore.V1.PATH_GROUPS)
            .headers { headers -> bearer?.let { headers.setBearerAuth(it) } }
            .bodyValue(input).retrieve()
            .onStatus({ it.isError }) { response -> Mono.error(UpstreamServiceException(response.statusCode().value(), "Expense Core returned HTTP ${response.statusCode().value()}")) }
            .bodyToMono(BffGroup::class.java).timeout(timeout)

    fun listGroups(bearer: String?): Mono<List<BffGroup>> =
        client.get().uri(ApiEndpoints.ExpenseCore.V1.PATH_GROUPS)
            .headers { headers -> bearer?.let { headers.setBearerAuth(it) } }
            .retrieve()
            .onStatus({ it.isError }) { response -> Mono.error(UpstreamServiceException(response.statusCode().value(), "Expense Core returned HTTP ${response.statusCode().value()}")) }
            .bodyToFlux(BffGroup::class.java)
            .collectList()
            .flatMap { groups ->
                Flux.fromIterable(groups)
                    .flatMapSequential({ group ->
                        listMembers(group.groupId, bearer)
                            .map { members -> group.copy(members = members) }
                    }, 4)
                    .collectList()
            }
            .timeout(timeout)

    fun updateGroup(groupId: String, name: String, bearer: String?): Mono<BffGroup> =
        client.patch().uri(ApiEndpoints.ExpenseCore.V1.PATH_GROUP_BY_ID, groupId)
            .headers { headers -> bearer?.let { headers.setBearerAuth(it) } }
            .bodyValue(mapOf("name" to name))
            .retrieve()
            .onStatus({ it.isError }) { response -> Mono.error(UpstreamServiceException(response.statusCode().value(), "Expense Core returned HTTP ${response.statusCode().value()}")) }
            .bodyToMono(BffGroup::class.java)
            .timeout(timeout)

    fun listMembers(groupId: String, bearer: String?): Mono<List<BffMember>> =
        client.get().uri(ApiEndpoints.ExpenseCore.V1.PATH_GROUP_MEMBERS, groupId)
            .headers { headers -> bearer?.let { headers.setBearerAuth(it) } }
            .retrieve()
            .onStatus({ it.isError }) { response -> Mono.error(UpstreamServiceException(response.statusCode().value(), "Expense Core returned HTTP ${response.statusCode().value()}")) }
            .bodyToFlux(BffMember::class.java)
            .collectList()
            .timeout(timeout)

    fun getGroup(groupId: String, bearer: String?): Mono<BffGroup> {
        val groupMono = client.get().uri(ApiEndpoints.ExpenseCore.V1.PATH_GROUP_BY_ID, groupId)
            .headers { headers -> bearer?.let { headers.setBearerAuth(it) } }
            .retrieve()
            .onStatus({ it.isError }) { response -> Mono.error(UpstreamServiceException(response.statusCode().value(), "Expense Core returned HTTP ${response.statusCode().value()}")) }
            .bodyToMono(BffGroup::class.java)

        val balancesMono = client.get().uri(ApiEndpoints.ExpenseCore.V1.PATH_GROUP_BALANCES, groupId)
            .headers { headers -> bearer?.let { headers.setBearerAuth(it) } }
            .retrieve()
            .onStatus({ it.isError }) { response -> Mono.error(UpstreamServiceException(response.statusCode().value(), "Expense Core returned HTTP ${response.statusCode().value()}")) }
            .bodyToMono(BffBalancesResponse::class.java)
            .map { it.balances }
            .onErrorReturn(emptyList())

        val expensesMono = client.get().uri(ApiEndpoints.ExpenseCore.V1.PATH_GROUP_EXPENSES, groupId)
            .headers { headers -> bearer?.let { headers.setBearerAuth(it) } }
            .retrieve()
            .onStatus({ it.isError }) { response -> Mono.error(UpstreamServiceException(response.statusCode().value(), "Expense Core returned HTTP ${response.statusCode().value()}")) }
            .bodyToFlux(BffExpense::class.java)
            .collectList()
            .onErrorReturn(emptyList())

        val membersMono = listMembers(groupId, bearer)

        return Mono.zip(groupMono, balancesMono, expensesMono, membersMono)
            .map { tuple ->
                val g = tuple.t1
                val balances = tuple.t2
                val expenses = tuple.t3
                val members = tuple.t4
                g.copy(balances = balances, expenses = expenses, members = members)
            }
            .timeout(timeout)
    }

    fun createExpense(groupId: String, input: CreateExpenseInput, idempotencyKey: String, bearer: String?): Mono<BffExpense> =
        client.post().uri(ApiEndpoints.ExpenseCore.V1.PATH_GROUP_EXPENSES, groupId)
            .header(ApiEndpoints.Headers.IDEMPOTENCY_KEY, idempotencyKey)
            .headers { headers -> bearer?.let { headers.setBearerAuth(it) } }
            .bodyValue(input)
            .retrieve()
            .onStatus({ it.isError }) { response -> Mono.error(UpstreamServiceException(response.statusCode().value(), "Expense Core returned HTTP ${response.statusCode().value()}")) }
            .bodyToMono(BffExpense::class.java)
            .timeout(timeout)

    fun recordRepayment(groupId: String, input: RepaymentInput, bearer: String?): Mono<BffSettlement> {
        val payload = mapOf(
            "fromParticipantId" to input.fromParticipantId,
            "toParticipantId" to input.toParticipantId,
            "amountMinor" to input.amount.minor
        )
        return client.post().uri(ApiEndpoints.ExpenseCore.V1.PATH_GROUP_SETTLEMENTS, groupId)
            .headers { headers -> bearer?.let { headers.setBearerAuth(it) } }
            .bodyValue(payload)
            .retrieve()
            .onStatus({ it.isError }) { response -> Mono.error(UpstreamServiceException(response.statusCode().value(), "Expense Core returned HTTP ${response.statusCode().value()}")) }
            .bodyToMono(BffSettlement::class.java)
            .map { it.copy(currency = input.amount.currency) }
            .timeout(timeout)
    }

    fun getSettlementSuggestions(groupId: String, bearer: String?): Mono<List<BffSuggestedSettlement>> =
        client.get().uri(ApiEndpoints.ExpenseCore.V1.PATH_GROUP_SETTLEMENT_SUGGESTIONS, groupId)
            .headers { headers -> bearer?.let { headers.setBearerAuth(it) } }
            .retrieve()
            .onStatus({ it.isError }) { response -> Mono.error(UpstreamServiceException(response.statusCode().value(), "Expense Core returned HTTP ${response.statusCode().value()}")) }
            .bodyToFlux(BffSuggestedSettlement::class.java)
            .collectList()
            .timeout(timeout)
}
