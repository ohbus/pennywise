package com.subhrodip.pennywise.accounts

import java.time.Instant
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

enum class DeletionStatus { REQUESTED, CANCELLED, COMPLETED }
data class DeletionRequest(val subject: String, val requestedAt: Instant, var status: DeletionStatus = DeletionStatus.REQUESTED)

@Service
class DeletionRequestService(
    private val store: DeletionRequestStore = InMemoryDeletionRequestStore()
) {
    constructor(clock: () -> Instant) : this(InMemoryDeletionRequestStore(clock))

    fun request(subject: String): DeletionRequest = store.request(subject)
    fun get(subject: String): DeletionRequest? = store.get(subject)
    fun cancel(subject: String): DeletionRequest? = store.cancel(subject)
    fun complete(subject: String): DeletionRequest? = store.complete(subject)
}
