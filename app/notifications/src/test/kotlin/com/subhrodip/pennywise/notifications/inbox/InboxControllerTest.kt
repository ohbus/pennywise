package com.subhrodip.pennywise.notifications.inbox

import com.subhrodip.pennywise.errors.GlobalErrorHandler
import com.subhrodip.pennywise.ids.ApiEndpoints
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.security.Principal
import java.time.Instant
import java.util.UUID

class InboxControllerTest {
    private val inbox = NotificationInbox()
    private val controller = InboxController(inbox)
    private val mvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(GlobalErrorHandler()).build()
    private val user = RequestPostProcessor { request -> request.userPrincipal = Principal { "alice" }; request }

    @Test
    fun `orders inbox newest first and isolates subjects`() {
        val testInbox = NotificationInbox()
        testInbox.append("alice", InboxItem(UUID.randomUUID(), "expense.created", "Expense", Instant.EPOCH))
        testInbox.append("alice", InboxItem(UUID.randomUUID(), "repayment.created", "Repayment", Instant.ofEpochSecond(2)))
        testInbox.append("bob", InboxItem(UUID.randomUUID(), "expense.created", "Other", Instant.now()))
        assertEquals("repayment.created", testInbox.list("alice").first().eventType)
        assertEquals(2, testInbox.list("alice").size)
        assertEquals(1, testInbox.list("bob").size)
    }

    @Test
    fun `returns stable cursor pages`() {
        val testInbox = NotificationInbox()
        repeat(3) { index -> testInbox.append("alice", InboxItem(UUID.randomUUID(), "event-$index", "Event", Instant.ofEpochSecond(index.toLong()))) }
        val first = testInbox.page("alice", null, 2)
        assertEquals(listOf("event-2", "event-1"), first.items.map { it.eventType })
        val second = testInbox.page("alice", first.nextCursor, 2)
        assertEquals(listOf("event-0"), second.items.map { it.eventType })
        assertEquals(null, second.nextCursor)
    }

    @Test
    fun `rejects malformed cursor`() {
        val testInbox = NotificationInbox()
        val err = org.junit.jupiter.api.Assertions.assertThrows(com.subhrodip.pennywise.errors.ApplicationException::class.java) {
            testInbox.page("alice", "bad", 10)
        }
        assertEquals(com.subhrodip.pennywise.errors.ErrorCode.ERR_02, err.errorCode)
    }

    @Test
    fun `marks existing notification as read returning 204`() {
        val notificationId = UUID.randomUUID()
        inbox.append("alice", InboxItem(notificationId, "expense.created", "Expense", Instant.now(), read = false))

        mvc.perform(post(ApiEndpoints.Notifications.V1.inboxMarkRead(notificationId)).with(user))
            .andExpect(status().isNoContent)

        val items = inbox.list("alice")
        assertEquals(1, items.size)
        assertTrue(items.first().read)
    }

    @Test
    fun `returns 404 when marking unknown notification as read`() {
        val unknownId = UUID.randomUUID()
        mvc.perform(post(ApiEndpoints.Notifications.V1.inboxMarkRead(unknownId)).with(user))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
    }

    @Test
    fun `returns 404 when marking another user notification as read`() {
        val notificationId = UUID.randomUUID()
        inbox.append("bob", InboxItem(notificationId, "expense.created", "Expense", Instant.now(), read = false))

        mvc.perform(post(ApiEndpoints.Notifications.V1.inboxMarkRead(notificationId)).with(user))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
    }

    @Test
    fun `rejects unauthenticated inbox listing`() {
        mvc.perform(get(ApiEndpoints.Notifications.V1.PATH_INBOX))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
    }

    @Test
    fun `rejects invalid inbox page limits as validation errors`() {
        mvc.perform(get(ApiEndpoints.Notifications.V1.PATH_INBOX).with(user).param("limit", "0"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
    }

    @Test
    fun `rejects malformed inbox cursors as validation errors`() {
        mvc.perform(
            get(ApiEndpoints.Notifications.V1.PATH_INBOX)
                .with(user)
                .param("cursor", "not-a-valid-cursor")
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
    }

    @Test
    fun `in memory store markAsRead updates status correctly`() {
        val store = InMemoryNotificationInboxStore()
        val id = UUID.randomUUID()
        store.append("alice", InboxItem(id, "expense.created", "Expense", Instant.now(), read = false))

        assertFalse(store.markAsRead("bob", id))
        assertFalse(store.markAsRead("alice", UUID.randomUUID()))
        assertTrue(store.markAsRead("alice", id))
        assertTrue(store.list("alice").first().read)
    }
}
