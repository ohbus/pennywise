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
    fun `graphql route returns errors for an invalid field`() {
        client.post().uri("/graphql")
            .bodyValue(mapOf("query" to "{ doesNotExist }"))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.errors").isArray
            .jsonPath("$.data").doesNotExist()
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
