package com.subhrodip.pennywise.expensecore.sync.persistence

/** Compatibility facade combining synchronization command and query ports. */
interface SynchronizationStore : SynchronizationCommandStore, SynchronizationQueryStore
