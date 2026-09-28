package com.subhrodip.squarewise.expensecore.sync.persistence

/** Compatibility facade combining synchronization command and query ports. */
interface SynchronizationStore : SynchronizationCommandStore, SynchronizationQueryStore
