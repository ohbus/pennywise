package com.subhrodip.pennywise.accounts.requests.deletion.persistence

import com.subhrodip.pennywise.accounts.requests.deletion.model.DeletionRequest
import com.subhrodip.pennywise.accounts.requests.deletion.model.DeletionStatus
import com.subhrodip.pennywise.accounts.profile.service.ProfileRules
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/** Thread-safe in-memory deletion-request adapter for local and test use. */
class InMemoryDeletionRequestStore(
    private val clock: () -> Instant = Instant::now
) : DeletionRequestStore {
    private val requests = ConcurrentHashMap<String, DeletionRequest>()

    override fun request(subject: String): DeletionRequest {
        ProfileRules.requireSubject(subject)
        return requests.compute(subject) { _, existing -> existing ?: DeletionRequest(subject, clock()) }!!
    }

    override fun get(subject: String): DeletionRequest? {
        ProfileRules.requireSubject(subject)
        return requests[subject]
    }

    override fun cancel(subject: String): DeletionRequest? {
        ProfileRules.requireSubject(subject)
        val existing = requests[subject] ?: return null
        return existing.copy(status = DeletionStatus.CANCELLED).also { requests[subject] = it }
    }

    override fun complete(subject: String): DeletionRequest? {
        ProfileRules.requireSubject(subject)
        val existing = requests[subject] ?: return null
        return existing.copy(status = DeletionStatus.COMPLETED).also { requests[subject] = it }
    }
}
