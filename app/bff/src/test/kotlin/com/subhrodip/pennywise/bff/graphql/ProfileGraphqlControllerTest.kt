package com.subhrodip.pennywise.bff.graphql

import com.subhrodip.pennywise.bff.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import reactor.core.publisher.Mono
import java.security.Principal
import java.util.UUID

class ProfileGraphqlControllerTest {

    private val accountsGateway = mock(AccountsGateway::class.java)
    private val controller = ProfileGraphqlController(accountsGateway)

    @Test
    fun `resolves me query using accounts gateway`() {
        val accountId = UUID.randomUUID().toString()
        val expected = BffProfile(
            accountId = accountId,
            displayName = "Alice",
            timezone = "Europe/Vienna",
            defaultCurrency = "EUR"
        )
        val principal = Principal { "alice" }

        `when`(accountsGateway.getMe("alice")).thenReturn(Mono.just(expected))

        val result = controller.me(principal).block()
        assertNotNull(result)
        assertEquals(accountId, result?.accountId)
        assertEquals("Alice", result?.displayName)
        assertEquals("Europe/Vienna", result?.timezone)
        assertEquals("EUR", result?.defaultCurrency)
    }
}
