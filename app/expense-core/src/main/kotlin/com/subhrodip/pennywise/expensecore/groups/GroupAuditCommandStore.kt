package com.subhrodip.pennywise.expensecore.groups

/**
 * Writer-side audit port used by group mutation transactions.
 *
 * Audit records are part of the same transaction as the aggregate mutation;
 * this port intentionally exposes no historical read or deletion operation.
 */
interface GroupAuditCommandStore {
    /** Persists one audit record before the enclosing mutation commits. */
    fun save(entity: GroupAuditEntity): GroupAuditEntity
}
