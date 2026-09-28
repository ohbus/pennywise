package com.subhrodip.pennywise.expensecore.sync.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

/**
 * JPA entity representing a synchronization change journal entry.
 *
 * Tracks incremental revisions per group for client delta synchronizations.
 */
@Entity
@Table(
    name = "sync_changes",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_sync_changes_group_revision",
            columnNames = ["group_id", "revision"]
        )
    ]
)
class SyncChangeEntity(
    @Id
    @Column(name = "change_id", nullable = false)
    var changeId: UUID,

    @Column(name = "group_id", nullable = false, length = 100)
    var groupId: String,

    @Column(name = "revision", nullable = false)
    var revision: Long,

    @Column(name = "entity_id", nullable = false, length = 100)
    var entityId: String,

    @Column(name = "deleted", nullable = false)
    var deleted: Boolean = false,

    @Column(name = "payload", columnDefinition = "TEXT")
    var payload: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)
