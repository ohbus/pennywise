package com.subhrodip.squarewise.security

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** Verifies reactive decoder construction fails before network discovery on incomplete config. */
class ReactiveOidcJwtDecoderFactoryTest {
    @Test
    fun `reactive decoder rejects blank issuer`() {
        assertThrows(IllegalArgumentException::class.java) {
            ReactiveOidcJwtDecoderFactory.create("", "squarewise-api")
        }
    }

    @Test
    fun `reactive decoder rejects blank audience`() {
        assertThrows(IllegalArgumentException::class.java) {
            ReactiveOidcJwtDecoderFactory.create("https://issuer.example", "")
        }
    }

    @Test
    fun `reactive issuer discovery factory builds a decoder from local metadata`() {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        val issuer = "http://127.0.0.1:${server.address.port}"
        val metadata = """
            {"issuer":"$issuer","jwks_uri":"$issuer/jwks"}
        """.trimIndent().toByteArray(StandardCharsets.UTF_8)
        server.createContext("/.well-known/openid-configuration") { exchange ->
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, metadata.size.toLong())
            exchange.responseBody.use { it.write(metadata) }
        }
        server.start()

        try {
            assertNotNull(ReactiveOidcJwtDecoderFactory.create(issuer, "squarewise-api"))
        } finally {
            server.stop(0)
        }
    }
}
