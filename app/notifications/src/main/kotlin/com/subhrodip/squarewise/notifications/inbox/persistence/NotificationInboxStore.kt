package com.subhrodip.squarewise.notifications.inbox.persistence

/** Compatibility facade for existing inbox adapters. */
interface NotificationInboxStore : NotificationInboxCommandStore, NotificationInboxQueryStore
