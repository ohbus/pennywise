package com.subhrodip.pennywise.notifications.inbox.persistence

/** Compatibility facade for existing inbox adapters. */
interface NotificationInboxStore : NotificationInboxCommandStore, NotificationInboxQueryStore
