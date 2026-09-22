package com.subhrodip.pennywise.notifications.preferences.persistence

import com.subhrodip.pennywise.notifications.preferences.model.NotificationPreferences
import java.util.concurrent.ConcurrentHashMap

/** Thread-safe in-memory preference adapter used by local runs and tests. */
class InMemoryPreferenceStore : PreferenceStore {
    private val preferences = ConcurrentHashMap<String, NotificationPreferences>()
    override fun get(subject: String): NotificationPreferences = preferences[subject] ?: NotificationPreferences()
    override fun put(subject: String, value: NotificationPreferences) { preferences[subject] = value }
}
