package com.subhrodip.pennywise.notifications.inbox.api

import com.subhrodip.pennywise.notifications.inbox.model.InboxPage
import com.subhrodip.pennywise.notifications.inbox.service.NotificationInboxService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.security.Principal
import com.subhrodip.pennywise.errors.domain.ApplicationException
import com.subhrodip.pennywise.errors.domain.ErrorCode
import java.util.UUID

import com.subhrodip.pennywise.ids.contracts.ApiEndpoints

@RestController
@RequestMapping(ApiEndpoints.Notifications.V1.PATH_INBOX)
class InboxController(private val inbox: NotificationInboxService) {
    @GetMapping
    fun list(
        principal: Principal?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "50") limit: Int
    ): InboxPage {
        val subject = principal?.name?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw ApplicationException(ErrorCode.ERR_03, "authenticated subject is required")
        require(limit in 1..100) { "limit must be between 1 and 100" }
        return inbox.page(subject, cursor, limit)
    }

    @PostMapping(ApiEndpoints.Notifications.V1.INBOX_MARK_READ_SUBPATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun markAsRead(@PathVariable notificationId: UUID, principal: Principal) {
        // Removed stray throw; method will perform normal logic.
        val subject = principal.name.trim().takeIf { it.isNotEmpty() }
            ?: throw ApplicationException(ErrorCode.ERR_03, "authenticated subject is required")
        if (!inbox.markAsRead(subject, notificationId)) {
            throw ApplicationException(ErrorCode.ERR_05, "Notification not found")
        }
    }
}
