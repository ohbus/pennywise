package com.subhrodip.squarewise.notifications.preferences.model

/** User-controlled delivery preferences for notification channels. */
data class NotificationPreferences(val emailEnabled: Boolean = true, val pushEnabled: Boolean = true)
