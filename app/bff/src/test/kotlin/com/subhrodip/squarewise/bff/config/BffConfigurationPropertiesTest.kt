package com.subhrodip.squarewise.bff.config

import com.subhrodip.squarewise.bff.messaging.config.BffMessagingProperties
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

/** Verifies BFF deployment properties retain their documented defaults and bound values. */
class BffConfigurationPropertiesTest {
    @Test
    fun `browser and GraphQL properties expose safe defaults`() {
        val abuse = GraphQlAbuseProperties()
        val browser = BrowserSessionProperties()
        val origins = BrowserOriginProperties()

        assertEquals(8, abuse.maxDepth)
        assertEquals(100, abuse.maxComplexity)
        assertEquals(20, abuse.maxSubscriptionsPerUser)
        assertEquals(64, abuse.subscriptionQueueCapacity)
        assertEquals(1800, abuse.subscriptionTtlSeconds)
        assertEquals("squarewise_access", browser.accessCookieName)
        assertEquals("squarewise_refresh", browser.refreshCookieName)
        assertEquals("squarewise_csrf", browser.csrfCookieName)
        assertEquals(Duration.ofDays(30), browser.cookieMaxAge)
        assertEquals(emptyList(), origins.allowedOrigins)
    }

    @Test
    fun `browser and messaging properties accept independently bound values`() {
        val abuse = GraphQlAbuseProperties()
        abuse.maxDepth = 12
        abuse.maxComplexity = 250
        abuse.maxSubscriptionsPerUser = 5
        abuse.subscriptionQueueCapacity = 10
        abuse.subscriptionTtlSeconds = 600

        val browser = BrowserSessionProperties()
        browser.accessCookieName = "access"
        browser.refreshCookieName = "refresh"
        browser.csrfCookieName = "csrf"
        browser.cookieMaxAge = Duration.ofHours(2)

        val origins = BrowserOriginProperties()
        origins.allowedOrigins = listOf("https://app.example.test")

        val messaging = BffMessagingProperties()
        messaging.enabled = true
        messaging.exchange = "events.test"
        messaging.routingKey = "events.all"
        messaging.deduplicatorCapacity = 25

        assertEquals(12, abuse.maxDepth)
        assertEquals(250, abuse.maxComplexity)
        assertEquals(5, abuse.maxSubscriptionsPerUser)
        assertEquals(10, abuse.subscriptionQueueCapacity)
        assertEquals(600, abuse.subscriptionTtlSeconds)
        assertEquals("access", browser.accessCookieName)
        assertEquals("refresh", browser.refreshCookieName)
        assertEquals("csrf", browser.csrfCookieName)
        assertEquals(Duration.ofHours(2), browser.cookieMaxAge)
        assertEquals(listOf("https://app.example.test"), origins.allowedOrigins)
        assertEquals(true, messaging.enabled)
        assertEquals("events.test", messaging.exchange)
        assertEquals("events.all", messaging.routingKey)
        assertEquals(25, messaging.deduplicatorCapacity)
    }
}
