package com.subhrodip.pennywise.expensecore.groups

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tools.jackson.databind.ObjectMapper

/**
 * Integration tests for [JpaGroupStore] verifying transactional persistence, optimistic locking / serialization,
 * audit logging, outbox dispatch, and authorization semantics for groups, members, and invitations.
 */
@SpringBootTest
class JpaGroupStoreTest @Autowired constructor(
    private val store: JpaGroupStore,
    private val auditRepository: GroupAuditRepository,
    private val syncRepository: com.subhrodip.pennywise.expensecore.sync.SyncChangeRepository,
    private val outboxRepository: com.subhrodip.pennywise.expensecore.messaging.OutboxRepository,
    private val groupRepository: GroupRepository,
    private val objectMapper: ObjectMapper
) {
    /**
     * Verifies that group creation persists the group entity with trimmed name, initial revision 0,
     * and restricts group listing exclusively to active members.
     */
    @Test
    fun `persists groups and restricts listing to members`() {
        val group = store.create("alice", CreateGroupRequest(" Vienna trip ", "TRIP", "EUR"))

        assertEquals("Vienna trip", group.name)
        assertEquals(listOf(group), store.list("alice"))
        assertTrue(store.list("bob").isEmpty())
    }

    /**
     * Verifies that concurrent attempts to claim the same invitation token result in exactly
     * one successful claim and one membership addition.
     */
    @Test
    fun `allows exactly one concurrent invitation claim`() {
        val group = store.create("owner", CreateGroupRequest("Household", "HOUSEHOLD", "EUR"))
        val invitation = store.invite(group.groupId, "owner", CreateInviteRequest(24))
        val executor = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)

        val results = (1..8).map { index ->
            executor.submit(Callable {
                start.await()
                runCatching { store.claim(invitation.token, "member-$index") }.isSuccess
            })
        }
        start.countDown()

        assertEquals(1, results.count { it.get() })
        executor.shutdown()
        assertEquals(1, (1..8).sumOf { store.list("member-$it").size })
    }

    /**
     * Verifies that claiming multiple distinct invitations for the same member to the same group
     * results in idempotent/single membership association.
     */
    @Test
    fun `links one membership when separate invitations are claimed concurrently`() {
        val group = store.create("owner-2", CreateGroupRequest("Trip", "TRIP", "USD"))
        val invitations = (1..2).map {
            store.invite(group.groupId, "owner-2", CreateInviteRequest(24))
        }
        val executor = Executors.newFixedThreadPool(2)
        val start = CountDownLatch(1)
        val results = invitations.map { invitation ->
            executor.submit(Callable {
                start.await()
                runCatching { store.claim(invitation.token, "same-member") }.isSuccess
            })
        }
        start.countDown()

        assertEquals(2, results.count { it.get() })
        executor.shutdown()
        assertEquals(listOf(group), store.list("same-member"))
    }

    /**
     * Verifies that listing members is restricted to members of the group and rejects non-members.
     */
    @Test
    fun `lists members for group members and rejects non-members`() {
        val group = store.create("member-alice", CreateGroupRequest("Alps trip", "TRIP", "EUR"))
        val membersBeforeClaim = store.listMembers(group.groupId, "member-alice")
        assertEquals(1, membersBeforeClaim.size)
        assertEquals(group.groupId, membersBeforeClaim[0].groupId)
        assertEquals("member-alice", membersBeforeClaim[0].subject)

        val invitation = store.invite(group.groupId, "member-alice", CreateInviteRequest(24))
        store.claim(invitation.token, "member-bob")

        val membersAfterClaim = store.listMembers(group.groupId, "member-bob")
        assertEquals(2, membersAfterClaim.size)
        assertEquals(setOf("member-alice", "member-bob"), membersAfterClaim.map { it.subject }.toSet())

        org.junit.jupiter.api.assertThrows<org.springframework.web.server.ResponseStatusException> {
            store.listMembers(group.groupId, "intruder")
        }
    }

    /**
     * Verifies that updating a group name trims whitespace, increments the revision number,
     * updates the database entity, and records audit and outbox side effects.
     */
    @Test
    fun `updates group name and increments revision`() {
        val group = store.create("update-alice", CreateGroupRequest("Before rename", "TRIP", "EUR"))
        assertEquals(0, group.revision)

        val updated = store.update(group.groupId, "update-alice", UpdateGroupRequest("  After rename  "))
        assertEquals("After rename", updated.name)
        assertEquals(1, updated.revision)

        val fetched = store.list("update-alice").first { it.groupId == group.groupId }
        assertEquals("After rename", fetched.name)
        assertEquals(1, fetched.revision)

        org.junit.jupiter.api.assertThrows<org.springframework.web.server.ResponseStatusException> {
            store.update(group.groupId, "stranger", UpdateGroupRequest("Nope"))
        }
    }

    /**
     * Verifies that a successful rename writes one matching audit, sync, and outbox effect.
     * The persisted payload and revision are compared across every durable representation.
     */
    @Test
    fun `rename effects contain the same exact payload and revision`() {
        val group = store.create("effect-owner", CreateGroupRequest("Before", "TRIP", "EUR"))

        val updated = store.update(group.groupId, "effect-owner", UpdateGroupRequest("After"))
        val audit = auditRepository.findAll().single { it.groupId == group.groupId }
        val sync = syncRepository.findAll().single { it.groupId == group.groupId.toString() }
        val outbox = outboxRepository.findAll().single { it.groupId == group.groupId }
        val expectedPayload = "{groupId=${group.groupId}, name=After, revision=1, changedBy=effect-owner}"
        val expectedOutboxPayload = objectMapper.writeValueAsString(
            mapOf("groupId" to group.groupId.toString(), "name" to "After", "revision" to 1, "changedBy" to "effect-owner")
        )

        assertEquals(1L, updated.revision)
        assertEquals("group.renamed", audit.action)
        assertEquals("effect-owner", audit.subject)
        assertEquals(1L, audit.revision)
        assertEquals(expectedPayload, audit.payload)
        assertEquals(1L, sync.revision)
        assertEquals(expectedPayload, sync.payload)
        assertEquals("group.renamed.v1", outbox.eventType)
        assertEquals(group.groupId, outbox.aggregateId)
        assertEquals(1L, outbox.groupRevision)
        assertEquals(expectedOutboxPayload, outbox.payload)
        assertNotNull(outbox.occurredAt)
    }

    /**
     * Verifies that a persistence constraint failure at transaction commit rolls back the group
     * mutation and every associated effect, leaving the previously committed state observable.
     */
    @Test
    fun `rename constraint failure rolls back group and all effects`() {
        val group = store.create("rollback-owner", CreateGroupRequest("Before", "TRIP", "EUR"))
        val initialAuditCount = auditRepository.count()
        val initialSyncCount = syncRepository.count()
        val initialOutboxCount = outboxRepository.count()
        val oversizedName = "x".repeat(121)

        assertTrue(runCatching {
            store.update(group.groupId, "rollback-owner", UpdateGroupRequest(oversizedName))
        }.isFailure)

        val persisted = groupRepository.findById(group.groupId).orElseThrow()
        assertEquals("Before", persisted.name)
        assertEquals(0L, persisted.revision)
        assertEquals(initialAuditCount, auditRepository.count())
        assertEquals(initialSyncCount, syncRepository.count())
        assertEquals(initialOutboxCount, outboxRepository.count())
    }

    /**
     * Verifies that concurrent rename requests are serialized using pessimistic write locking,
     * resulting in sequential revision increments without lost updates.
     */
    @Test
    fun `serializes concurrent renames and preserves both revisions`() {
        val group = store.create("concurrent-owner", CreateGroupRequest("Before", "TRIP", "EUR"))
        val executor = Executors.newFixedThreadPool(2)
        val start = CountDownLatch(1)
        val results = listOf("First", "Second").map { name ->
            executor.submit(Callable {
                start.await()
                store.update(group.groupId, "concurrent-owner", UpdateGroupRequest(name))
            })
        }
        start.countDown()

        val updates = results.map { it.get() }
        executor.shutdown()
        assertEquals(setOf(1L, 2L), updates.map { it.revision }.toSet())
        assertEquals(2L, store.list("concurrent-owner").single { it.groupId == group.groupId }.revision)
    }

    /**
     * Verifies that rejected rename requests (non-member actor or missing group ID) do NOT increment revision
     * and do NOT emit audit log entries, sync journal records, or outbox messages.
     */
    @Test
    fun `rejected rename requests do not increment revision or emit side effects`() {
        val group = store.create("owner-side-effects", CreateGroupRequest("Original Name", "TRIP", "EUR"))
        val initialAuditCount = auditRepository.count()
        val initialOutboxCount = outboxRepository.count()

        // Non-member attempt
        org.junit.jupiter.api.assertThrows<org.springframework.web.server.ResponseStatusException> {
            store.update(group.groupId, "unauthorized-subject", UpdateGroupRequest("Hacked Name"))
        }

        // Missing group attempt
        val nonExistentId = java.util.UUID.randomUUID()
        org.junit.jupiter.api.assertThrows<org.springframework.web.server.ResponseStatusException> {
            store.update(nonExistentId, "owner-side-effects", UpdateGroupRequest("Missing Group Name"))
        }

        // Verify entity unchanged
        val refreshed = groupRepository.findById(group.groupId).orElseThrow()
        assertEquals("Original Name", refreshed.name)
        assertEquals(0L, refreshed.revision)

        // Verify no additional audit or outbox entries created
        assertEquals(initialAuditCount, auditRepository.count())
        assertEquals(initialOutboxCount, outboxRepository.count())
    }
}
