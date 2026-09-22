package com.subhrodip.pennywise.accounts.requests.deletion.service

import com.subhrodip.pennywise.accounts.requests.deletion.model.DeletionRequest
import com.subhrodip.pennywise.accounts.requests.deletion.persistence.DeletionRequestStore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DeletionRequestService @Autowired constructor(
    private val store: DeletionRequestStore
) {
    fun request(subject: String): DeletionRequest = store.request(subject)
    fun get(subject: String): DeletionRequest? = store.get(subject)
    fun cancel(subject: String): DeletionRequest? = store.cancel(subject)
    fun complete(subject: String): DeletionRequest? = store.complete(subject)
}
