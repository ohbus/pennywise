package com.subhrodip.pennywise.expensecore.sync.persistence

/** Writer-side persistence port for synchronization changes. */
interface SynchronizationCommandStore {
    fun append(entityId: String, payload: String?): Long = append("default", entityId, payload)
    fun append(groupId: String, entityId: String, payload: String?): Long
    fun delete(entityId: String): Long = delete("default", entityId)
    fun delete(groupId: String, entityId: String): Long
}
