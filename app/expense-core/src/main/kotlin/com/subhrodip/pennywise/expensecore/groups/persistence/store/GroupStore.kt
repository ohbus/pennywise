package com.subhrodip.pennywise.expensecore.groups.persistence.store
/**
 * Compatibility facade combining group command and query ports.
 */
interface GroupStore : GroupCommandStore, GroupQueryStore
