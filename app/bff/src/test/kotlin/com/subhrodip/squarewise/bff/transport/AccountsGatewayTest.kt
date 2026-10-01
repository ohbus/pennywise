package com.subhrodip.squarewise.bff.transport

import com.subhrodip.squarewise.ids.contracts.ApiEndpoints
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.Exceptions

/** Verifies the Accounts gateway's optional bearer and upstream error boundaries over HTTP. */
class AccountsGatewayTest {
    private lateinit var server: HttpServer
    private lateinit var executor: ExecutorService
    private lateinit var gateway: AccountsGateway

    @BeforeEach
    fun setUp() {
        server = HttpServer.create(InetSocketAddress(0), 0)
        executor = Executors.newCachedThreadPool()
        server.executor = executor
        server.start()
        gateway = AccountsGateway(
            builder = WebClient.builder(),
            baseUrl = "http://localhost:${server.address.port}",
            timeout = Duration.ofSeconds(2)
        )
    }

    @AfterEach
    fun tearDown() {
        server.stop(0)
        executor.shutdownNow()
        executor.awaitTermination(5, TimeUnit.SECONDS)
    }

    /** Verifies both optional-bearer branches and the stable profile mapping. */
    @Test
    fun `gets profile with or without optional bearer`() {
        val authorizationHeaders = mutableListOf<String?>()
        server.createContext(ApiEndpoints.Accounts.V1.PATH_ME) { exchange ->
            authorizationHeaders += exchange.requestHeaders.getFirst(ApiEndpoints.Headers.AUTHORIZATION)
            respond(exchange, 200, """
                {"accountId":"account-1","displayName":"Alice","timezone":"UTC","defaultCurrency":"EUR"}
            """.trimIndent())
        }

        val withoutBearer = gateway.getMe(null).block()
        val withBearer = gateway.getMe("access-token").block()

        assertThat(withoutBearer?.accountId).isEqualTo("account-1")
        assertThat(withBearer?.displayName).isEqualTo("Alice")
        assertThat(authorizationHeaders).containsExactly(null, "Bearer access-token")
    }

    /** Verifies upstream status mapping does not expose the upstream response body. */
    @Test
    fun `maps upstream profile failure to the stable gateway exception`() {
        server.createContext(ApiEndpoints.Accounts.V1.PATH_ME) { exchange ->
            respond(exchange, 503, "internal account details must not escape")
        }

        val thrown = assertThrows<RuntimeException> { gateway.getMe("access-token").block() }
        val cause = Exceptions.unwrap(thrown)
        assertThat(cause).isInstanceOf(UpstreamServiceException::class.java)
        assertThat((cause as UpstreamServiceException).status).isEqualTo(503)
        assertThat(cause.message).doesNotContain("internal account details")
    }

    private fun respond(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.set(ApiEndpoints.Headers.CONTENT_TYPE, ApiEndpoints.Headers.APPLICATION_JSON)
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}
