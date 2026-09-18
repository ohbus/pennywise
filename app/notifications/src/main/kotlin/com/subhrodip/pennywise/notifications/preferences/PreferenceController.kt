package com.subhrodip.pennywise.notifications

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.security.Principal
import java.util.concurrent.ConcurrentHashMap

import com.subhrodip.pennywise.ids.ApiEndpoints

data class NotificationPreferences(val emailEnabled: Boolean = true, val pushEnabled: Boolean = true)

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
        ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "authenticated subject is required")
}

interface PreferenceStore {
    fun get(subject: String): NotificationPreferences
    fun put(subject: String, value: NotificationPreferences)
}

class InMemoryPreferenceStore : PreferenceStore {
    private val preferences = ConcurrentHashMap<String, NotificationPreferences>()

    override fun get(subject: String): NotificationPreferences =
        preferences[subject] ?: NotificationPreferences()

    override fun put(subject: String, value: NotificationPreferences) {
        preferences[subject] = value
    }
}
