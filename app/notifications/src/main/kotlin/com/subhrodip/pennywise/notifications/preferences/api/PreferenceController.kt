package com.subhrodip.pennywise.notifications.preferences.api

import com.subhrodip.pennywise.notifications.preferences.model.NotificationPreferences
import com.subhrodip.pennywise.notifications.preferences.persistence.PreferenceStore
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import jakarta.validation.Valid
import com.subhrodip.pennywise.errors.domain.ApplicationException
import com.subhrodip.pennywise.errors.domain.ErrorCode
import java.security.Principal

import com.subhrodip.pennywise.ids.contracts.ApiEndpoints

@RestController
@RequestMapping(ApiEndpoints.Notifications.V1.PATH_PREFERENCES)
class PreferenceController(private val store: PreferenceStore) {
    @GetMapping
    fun get(principal: Principal?): NotificationPreferences = store.get(subject(principal))

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun update(principal: Principal?, @Valid @RequestBody preferences: NotificationPreferences) {
        store.put(subject(principal), preferences)
    }

    private fun subject(principal: Principal?): String = principal?.name?.trim()?.takeIf { it.isNotEmpty() }
        ?: throw ApplicationException(ErrorCode.ERR_03, "authenticated subject is required")
}
