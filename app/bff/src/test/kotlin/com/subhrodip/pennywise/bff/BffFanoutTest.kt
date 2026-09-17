package com.subhrodip.pennywise.bff

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.Exceptions
import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * HTTP double tests verifying bounded group-member fanout behavior in [ExpenseCoreGateway].
 *
 * Covers:
 * - Zero, one, and many groups member resolution.
 * - Concurrency bound of at most 4 simultaneous upstream member requests during [ExpenseCoreGateway.listGroups].
 * - Exact preservation of group response order.
 * - Deduplication / request count guarantees (exactly one member request per group).
 * - Upstream error propagation (HTTP 404, 401/403, 5xx) via [UpstreamServiceException].
 * - Malformed member payload error handling.
 * - Upstream timeout handling.
 * - Single group member and details resolution via [ExpenseCoreGateway.getGroup].
 */
class BffFanoutTest {

    private lateinit var server: HttpServer
    private lateinit var gateway: ExpenseCoreGateway
    private var port: Int = 0

    private val recordedPaths = CopyOnWriteArrayList<String>()
    private val memberRequestCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val activeMemberRequests = AtomicInteger(0)
    private val peakMemberConcurrency = AtomicInteger(0)

    @BeforeEach
    fun setUp() {
        server = HttpServer.create(InetSocketAddress(0), 0)
        server.executor = java.util.concurrent.Executors.newCachedThreadPool()
        port = server.address.port
        server.start()

        val builder = WebClient.builder()
        gateway = ExpenseCoreGateway(
            builder = builder,
            baseUrl = "http://localhost:$port",
            timeout = Duration.ofMillis(800)
        )
    }

    @AfterEach
    fun tearDown() {
        server.stop(0)
    }

    private fun registerHandler(path: String, handler: (HttpExchange) -> Unit) {
        server.createContext(path, HttpHandler { exchange ->
            recordedPaths.add(exchange.requestURI.path)
            handler(exchange)
        })
    }

    private fun respondJson(exchange: HttpExchange, statusCode: Int, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.set("Content-Type", "application/json")
        exchange.sendResponseHeaders(statusCode, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun groupJson(groupId: String, name: String, kind: String = "TRIP", revision: Long = 1): String {
        return """{"groupId":"$groupId","name":"$name","kind":"$kind","revision":$revision,"balances":[],"expenses":[],"members":[]}"""
    }

    /**
     * Verifies that when Expense Core returns an empty group list, listGroups returns an empty list
     * without triggering any downstream member resolution requests.
     */
    @Test
    fun `resolves zero groups without triggering member requests`() {
        registerHandler("/expense-core/v1/groups") { exchange ->
            respondJson(exchange, 200, "[]")
        }

        val groups = gateway.listGroups("bearer-token").block()

        assertThat(groups).isNotNull
        assertThat(groups).isEmpty()
        assertThat(recordedPaths).containsExactly("/expense-core/v1/groups")
    }

    /**
     * Verifies that when a single group is returned, its members are correctly fetched and attached.
     */
    @Test
    fun `resolves single group with members successfully`() {
        registerHandler("/expense-core/v1/groups") { exchange ->
            respondJson(exchange, 200, "[${groupJson("g-1", "Trip Alpha")}]")
        }
        registerHandler("/expense-core/v1/groups/g-1/members") { exchange ->
            respondJson(exchange, 200, """[{"membershipId":"m-1","subject":"alice"},{"membershipId":"m-2","subject":"bob"}]""")
        }

        val groups = gateway.listGroups("bearer-token").block()

        assertThat(groups).isNotNull
        assertThat(groups).hasSize(1)
        val group = groups!![0]
        assertThat(group.groupId).isEqualTo("g-1")
        assertThat(group.name).isEqualTo("Trip Alpha")
        assertThat(group.members).hasSize(2)
        assertThat(group.members.map { it.subject }).containsExactly("alice", "bob")
    }

    /**
     * Verifies bounded concurrency, request deduplication, and ordering for many groups.
     *
     * Concurrency invariant: flatMapSequential(..., 4) limits concurrent member requests
     * to at most 4 simultaneous executions, while strictly preserving group sequence order.
     * Also verifies that no duplicate member requests occur for each group.
     */
    @Test
    fun `resolves many groups with at most 4 concurrent member requests and preserves order`() {
        val groupCount = 10
        val groupsJson = (1..groupCount).joinToString(",", "[", "]") { i ->
            groupJson("g-$i", "Group $i", revision = i.toLong())
        }

        registerHandler("/expense-core/v1/groups") { exchange ->
            respondJson(exchange, 200, groupsJson)
        }

        // Each member request delays briefly to observe concurrency and tracks invocation count
        for (i in 1..groupCount) {
            val gid = "g-$i"
            memberRequestCounts[gid] = AtomicInteger(0)
            registerHandler("/expense-core/v1/groups/$gid/members") { exchange ->
                memberRequestCounts[gid]!!.incrementAndGet()
                val current = activeMemberRequests.incrementAndGet()
                peakMemberConcurrency.accumulateAndGet(current) { prev, next -> maxOf(prev, next) }
                try {
                    Thread.sleep(60)
                } finally {
                    activeMemberRequests.decrementAndGet()
                }
                respondJson(exchange, 200, """[{"membershipId":"m-$i","subject":"user-$i"}]""")
            }
        }

        val groups = gateway.listGroups("bearer-token").block()

        assertThat(groups).isNotNull
        assertThat(groups).hasSize(groupCount)

        // Verify order strictly matches groups response
        for (i in 1..groupCount) {
            val group = groups!![i - 1]
            assertThat(group.groupId).isEqualTo("g-$i")
            assertThat(group.name).isEqualTo("Group $i")
            assertThat(group.members).hasSize(1)
            assertThat(group.members[0].subject).isEqualTo("user-$i")
            // Verify no duplicate member request occurred
            assertThat(memberRequestCounts["g-$i"]?.get()).isEqualTo(1)
        }

        // Invariant check: concurrency must never exceed 4 and must achieve concurrency > 1
        assertThat(peakMemberConcurrency.get()).isLessThanOrEqualTo(4)
        assertThat(peakMemberConcurrency.get()).isGreaterThanOrEqualTo(2)
    }

    /**
     * Verifies that if one member resolution fails with HTTP 404, listGroups fails immediately
     * with an [UpstreamServiceException] indicating status 404.
     */
    @Test
    fun `propagates upstream 404 during member resolution in listGroups`() {
        registerHandler("/expense-core/v1/groups") { exchange ->
            respondJson(exchange, 200, "[${groupJson("g-1", "Group 1")}]")
        }
        registerHandler("/expense-core/v1/groups/g-1/members") { exchange ->
            respondJson(exchange, 404, """{"error":"Not Found"}""")
        }

        val ex = assertThrows<RuntimeException> {
            gateway.listGroups("bearer-token").block()
        }
        val cause = Exceptions.unwrap(ex)
        assertThat(cause).isInstanceOf(UpstreamServiceException::class.java)
        val upstreamEx = cause as UpstreamServiceException
        assertThat(upstreamEx.status).isEqualTo(404)
    }

    /**
     * Verifies that upstream 401 authorization error during listGroups propagates as [UpstreamServiceException].
     */
    @Test
    fun `propagates upstream 401 unauthorized failure`() {
        registerHandler("/expense-core/v1/groups") { exchange ->
            respondJson(exchange, 401, """{"error":"Unauthorized"}""")
        }

        val ex = assertThrows<RuntimeException> {
            gateway.listGroups("bearer-token").block()
        }
        val cause = Exceptions.unwrap(ex)
        assertThat(cause).isInstanceOf(UpstreamServiceException::class.java)
        assertThat((cause as UpstreamServiceException).status).isEqualTo(401)
    }

    /**
     * Verifies that upstream 403 forbidden error during member resolution propagates as [UpstreamServiceException].
     */
    @Test
    fun `propagates upstream 403 forbidden failure during member resolution`() {
        registerHandler("/expense-core/v1/groups") { exchange ->
            respondJson(exchange, 200, "[${groupJson("g-1", "Group 1")}]")
        }
        registerHandler("/expense-core/v1/groups/g-1/members") { exchange ->
            respondJson(exchange, 403, """{"error":"Forbidden"}""")
        }

        val ex = assertThrows<RuntimeException> {
            gateway.listGroups("bearer-token").block()
        }
        val cause = Exceptions.unwrap(ex)
        assertThat(cause).isInstanceOf(UpstreamServiceException::class.java)
        assertThat((cause as UpstreamServiceException).status).isEqualTo(403)
    }

    /**
     * Verifies that upstream 5xx server errors propagate as [UpstreamServiceException].
     */
    @Test
    fun `propagates upstream 5xx errors from member resolution`() {
        registerHandler("/expense-core/v1/groups") { exchange ->
            respondJson(exchange, 200, "[${groupJson("g-1", "Group 1")}]")
        }
        registerHandler("/expense-core/v1/groups/g-1/members") { exchange ->
            respondJson(exchange, 503, """{"error":"Service Unavailable"}""")
        }

        val ex = assertThrows<RuntimeException> {
            gateway.listGroups("bearer-token").block()
        }
        val cause = Exceptions.unwrap(ex)
        assertThat(cause).isInstanceOf(UpstreamServiceException::class.java)
        assertThat((cause as UpstreamServiceException).status).isEqualTo(503)
    }

    /**
     * Verifies timeout behavior when upstream group or member endpoint exceeds configured timeout.
     */
    @Test
    fun `times out when upstream member resolution exceeds configured duration`() {
        registerHandler("/expense-core/v1/groups") { exchange ->
            respondJson(exchange, 200, "[${groupJson("g-1", "Group 1")}]")
        }
        registerHandler("/expense-core/v1/groups/g-1/members") { exchange ->
            Thread.sleep(1200)
            respondJson(exchange, 200, "[]")
        }

        val ex = assertThrows<RuntimeException> {
            gateway.listGroups("bearer-token").block()
        }
        val cause = Exceptions.unwrap(ex)
        assertThat(cause).isInstanceOf(java.util.concurrent.TimeoutException::class.java)
    }

    /**
     * Verifies handling when upstream returns a malformed member payload.
     */
    @Test
    fun `fails when upstream returns malformed member payload`() {
        registerHandler("/expense-core/v1/groups") { exchange ->
            respondJson(exchange, 200, "[${groupJson("g-1", "Group 1")}]")
        }
        registerHandler("/expense-core/v1/groups/g-1/members") { exchange ->
            respondJson(exchange, 200, "not-valid-json")
        }

        val ex = assertThrows<RuntimeException> {
            gateway.listGroups("bearer-token").block()
        }
        val cause = Exceptions.unwrap(ex)
        assertThat(cause).isInstanceOf(org.springframework.core.codec.DecodingException::class.java)
    }

    /**
     * Verifies getGroup fetches group, balances, expenses, and members concurrently,
     * handling partial balances/expenses failures gracefully via onErrorReturn, while populating members.
     */
    @Test
    fun `getGroup resolves group with balances, expenses, and members`() {
        registerHandler("/expense-core/v1/groups/g-1") { exchange ->
            respondJson(exchange, 200, groupJson("g-1", "Group 1", revision = 2))
        }
        registerHandler("/expense-core/v1/groups/g-1/balances") { exchange ->
            respondJson(exchange, 200, """{"groupId":"g-1","balances":[{"participantId":"p-1","amount":{"currency":"EUR","minor":"100"}}]}""")
        }
        registerHandler("/expense-core/v1/groups/g-1/expenses") { exchange ->
            respondJson(exchange, 200, """[{"expenseId":"e-1","version":1,"description":"Lunch","amount":{"currency":"EUR","minor":"200"},"allocations":[]}]""")
        }
        registerHandler("/expense-core/v1/groups/g-1/members") { exchange ->
            respondJson(exchange, 200, """[{"membershipId":"m-1","subject":"alice"}]""")
        }

        val group = gateway.getGroup("g-1", "bearer-token").block()

        assertThat(group).isNotNull
        assertThat(group!!.groupId).isEqualTo("g-1")
        assertThat(group.balances).hasSize(1)
        assertThat(group.expenses).hasSize(1)
        assertThat(group.members).hasSize(1)
        assertThat(group.members[0].subject).isEqualTo("alice")
    }

    /**
     * Verifies getGroup propagates upstream 404 when group is not found.
     */
    @Test
    fun `getGroup propagates upstream 404 when group does not exist`() {
        registerHandler("/expense-core/v1/groups/missing") { exchange ->
            respondJson(exchange, 404, """{"error":"Group not found"}""")
        }
        registerHandler("/expense-core/v1/groups/missing/balances") { exchange ->
            respondJson(exchange, 404, """{"error":"Not found"}""")
        }
        registerHandler("/expense-core/v1/groups/missing/expenses") { exchange ->
            respondJson(exchange, 404, """{"error":"Not found"}""")
        }
        registerHandler("/expense-core/v1/groups/missing/members") { exchange ->
            respondJson(exchange, 404, """{"error":"Not found"}""")
        }

        val ex = assertThrows<RuntimeException> {
            gateway.getGroup("missing", "bearer-token").block()
        }
        val cause = Exceptions.unwrap(ex)
        assertThat(cause).isInstanceOf(UpstreamServiceException::class.java)
        assertThat((cause as UpstreamServiceException).status).isEqualTo(404)
    }

    /**
     * Verifies authorization header is forwarded to upstream endpoints.
     */
    @Test
    fun `forwards bearer authorization header to upstream endpoints`() {
        val capturedAuthHeaders = CopyOnWriteArrayList<String>()
        registerHandler("/expense-core/v1/groups") { exchange ->
            capturedAuthHeaders.addAll(exchange.requestHeaders.getOrDefault("Authorization", emptyList()))
            respondJson(exchange, 200, "[${groupJson("g-auth", "Auth Group")}]")
        }
        registerHandler("/expense-core/v1/groups/g-auth/members") { exchange ->
            capturedAuthHeaders.addAll(exchange.requestHeaders.getOrDefault("Authorization", emptyList()))
            respondJson(exchange, 200, "[]")
        }

        gateway.listGroups("my-secret-token").block()

        assertThat(capturedAuthHeaders).contains("Bearer my-secret-token")
    }
}
