package com.subhrodip.pennywise.bff

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.`when`
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.context.bean.override.mockito.MockitoBean
import reactor.core.publisher.Mono
import java.util.concurrent.TimeoutException
import java.util.UUID

/** Verifies the real WebFlux GraphQL HTTP handler and its response envelope. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration," +
            "org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration," +
            "org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration"
    ]
)
class GraphqlHttpTransportTest {
    @TestConfiguration
    class PermitAllSecurity {
        @Bean
        fun testSecurity(http: ServerHttpSecurity): SecurityWebFilterChain =
            http.csrf { it.disable() }.authorizeExchange { it.anyExchange().permitAll() }.build()
    }

    @LocalServerPort
    private var port: Int = 0

    private lateinit var client: WebTestClient

    @BeforeEach
    fun setUpClient() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
    }

    @MockitoBean
    private lateinit var accountsGateway: AccountsGateway

    @MockitoBean
    private lateinit var expenseCoreGateway: ExpenseCoreGateway

    @Test
    fun `graphql route returns data for a profile query`() {
        `when`(accountsGateway.getMe(null)).thenReturn(
            Mono.just(BffProfile("account-1", "Alice", "Europe/Vienna", "EUR"))
        )

        client.post().uri("/graphql")
            .bodyValue(mapOf("query" to "{ me { accountId displayName } }"))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data.me.accountId").isEqualTo("account-1")
            .jsonPath("$.data.me.displayName").isEqualTo("Alice")
    }

    @Test
    fun `graphql route exposes group and settlement suggestion queries`() {
        val group = BffGroup("group-1", "Trip", "TRIP", revision = 2)
        `when`(expenseCoreGateway.listGroups(null)).thenReturn(Mono.just(listOf(group)))
        `when`(expenseCoreGateway.getSettlementSuggestions("group-1", null)).thenReturn(
            Mono.just(listOf(BffSuggestedSettlement("alice", "bob", 1250, "EUR")))
        )

        client.post().uri("/graphql")
            .bodyValue(mapOf("query" to "{ groups { id name revision } settlementSuggestions(groupId: \"group-1\") { fromParticipantId amount { minor currency } } }"))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data.groups[0].id").isEqualTo("group-1")
            .jsonPath("$.data.settlementSuggestions[0].amount.minor").isEqualTo("1250")
    }

    @Test
    fun `graphql route exposes group and repayment mutations`() {
        val group = BffGroup("group-2", "Household", "HOUSEHOLD", revision = 1)
        val settlement = BffSettlement("settlement-1", "alice", "bob", 500, "RECORDED", "EUR")
        `when`(expenseCoreGateway.createGroup(BffCreateGroup("Household", "HOUSEHOLD", "EUR"), null))
            .thenReturn(Mono.just(group))
        `when`(expenseCoreGateway.recordRepayment("group-2", RepaymentInput("group-2", "alice", "bob", MoneyInput("EUR", "500"), "repaid"), null))
            .thenReturn(Mono.just(settlement))

        client.post().uri("/graphql")
            .bodyValue(mapOf("query" to "mutation { createGroup(input: { name: \"Household\", kind: HOUSEHOLD, currency: \"EUR\" }) { id name } recordRepayment(input: { groupId: \"group-2\", fromParticipantId: \"alice\", toParticipantId: \"bob\", amount: { currency: \"EUR\", minor: \"500\" }, reason: \"repaid\" }) { id status amount { minor currency } } }"))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data.createGroup.id").isEqualTo("group-2")
            .jsonPath("$.data.recordRepayment.amount.minor").isEqualTo("500")
    }

    @Test
    fun `graphql route exposes group update and expense creation mutations`() {
        val group = BffGroup("group-3", "Renamed", "TRIP", revision = 3)
        val expenseId = UUID.randomUUID().toString()
        val input = CreateExpenseInput(
            expenseId,
            "Dinner",
            MoneyInput("EUR", "1000"),
            listOf(PayerInput("alice", MoneyInput("EUR", "1000"))),
            AllocationInput("EQUAL", listOf(AllocationItemInput("bob", "1")))
        )
        val expense = BffExpense(expenseId, 1, "Dinner", BffMoney("EUR", "1000"))
        `when`(expenseCoreGateway.updateGroup("group-3", "Renamed", null)).thenReturn(Mono.just(group))
        `when`(expenseCoreGateway.createExpense("group-3", input, "idempotency-key-1234", null))
            .thenReturn(Mono.just(expense))

        client.post().uri("/graphql")
            .bodyValue(mapOf("query" to "mutation { updateGroup(groupId: \"group-3\", name: \"Renamed\") { id name revision } }"))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data.updateGroup.name").isEqualTo("Renamed")

        client.post().uri("/graphql")
            .bodyValue(
                mapOf(
                    "query" to "mutation { createExpense(groupId: \"group-3\", input: { expenseId: \"$expenseId\", description: \"Dinner\", " +
                        "amount: { currency: \"EUR\", minor: \"1000\" }, payers: [{ participantId: \"alice\", amount: { currency: \"EUR\", minor: \"1000\" } }], " +
                        "allocation: { mode: \"EQUAL\", items: [{ participantId: \"bob\", value: \"1\" }] } }, " +
                        "idempotencyKey: \"idempotency-key-1234\") { id description amount { minor currency } } }"
                )
            )
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data.createExpense.id").isEqualTo(expenseId)
            .jsonPath("$.data.createExpense.amount.minor").isEqualTo("1000")
    }

    @Test
    fun `graphql route rejects malformed required variables before calling upstream`() {
        client.post().uri("/graphql")
            .bodyValue(mapOf("query" to "mutation { createExpense(groupId: \"not-a-uuid\", input: { expenseId: \"${UUID.randomUUID()}\", description: \"Dinner\", amount: { currency: \"EUR\", minor: \"100\" }, payers: [], allocation: { mode: EQUAL, items: [] } }, idempotencyKey: \"short\") { id } }"))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.errors").isArray
            .jsonPath("$.data").doesNotExist()
    }

    @Test
    fun `graphql route returns errors for an invalid field`() {
        client.post().uri("/graphql")
            .bodyValue(mapOf("query" to "{ doesNotExist }"))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.errors").isArray
            .jsonPath("$.data").doesNotExist()
    }

    /**
     * Verifies malformed JSON is rejected by the HTTP transport before GraphQL
     * execution; no upstream gateway call should be required for this failure.
     */
    @Test
    fun `graphql route rejects malformed json payload`() {
        client.post().uri("/graphql")
            .header("Content-Type", "application/json")
            .bodyValue("{\"query\":")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `upstream authorization failure remains a GraphQL error without leaking detail`() {
        `when`(expenseCoreGateway.getGroup("group-1", null))
            .thenReturn(Mono.error(UpstreamServiceException(403, "private authorization detail")))

        client.post().uri("/graphql")
            .bodyValue(mapOf("query" to "{ group(id: \"group-1\") { id } }"))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.errors").isArray
            .jsonPath("$.errors[0].message").value<String> { message ->
                assert(!message.contains("private authorization detail"))
            }
    }

    @Test
    fun `upstream validation and timeout failures use the GraphQL error envelope`() {
        `when`(expenseCoreGateway.updateGroup("group-1", "", null))
            .thenReturn(Mono.error(UpstreamServiceException(400, "private validation detail")))

        client.post().uri("/graphql")
            .bodyValue(mapOf("query" to "mutation { updateGroup(groupId: \"group-1\", name: \"\") { id } }"))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.errors").isArray
            .jsonPath("$.errors[0].message").value<String> { message ->
                assert(!message.contains("private validation detail"))
            }

        `when`(expenseCoreGateway.getGroup("group-2", null))
            .thenReturn(Mono.error(TimeoutException("private timeout detail")))

        client.post().uri("/graphql")
            .bodyValue(mapOf("query" to "{ group(id: \"group-2\") { id } }"))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.errors").isArray
            .jsonPath("$.errors[0].message").value<String> { message ->
                assert(!message.contains("private timeout detail"))
            }
    }

    @Test
    fun `malformed upstream response is represented by a GraphQL error`() {
        `when`(expenseCoreGateway.getGroup("group-3", null))
            .thenReturn(Mono.error(IllegalStateException("private malformed payload")))

        client.post().uri("/graphql")
            .bodyValue(mapOf("query" to "{ group(id: \"group-3\") { id } }"))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.errors").isArray
            .jsonPath("$.errors[0].message").value<String> { message ->
                assert(!message.contains("private malformed payload"))
            }
    }
}
