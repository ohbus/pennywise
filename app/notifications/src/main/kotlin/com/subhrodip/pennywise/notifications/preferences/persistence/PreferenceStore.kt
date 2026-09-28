package com.subhrodip.pennywise.notifications.preferences.persistence

import com.subhrodip.pennywise.notifications.preferences.model.NotificationPreferences
/** Port for reading and updating per-subject notification preferences. */
interface PreferenceStore {
    fun get(subject: String): NotificationPreferences
    fun put(subject: String, value: NotificationPreferences)
}
