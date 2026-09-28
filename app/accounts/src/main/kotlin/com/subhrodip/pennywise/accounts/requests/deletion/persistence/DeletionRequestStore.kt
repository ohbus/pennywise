package com.subhrodip.pennywise.accounts.requests.deletion.persistence

import com.subhrodip.pennywise.accounts.requests.deletion.model.DeletionRequest
/** Persistence port for account deletion requests. */
interface DeletionRequestStore {
    /** Submits or retrieves the current request for a subject. */
    fun request(subject: String): DeletionRequest
    /** Retrieves the current request, if present. */
    fun get(subject: String): DeletionRequest?
    /** Cancels an existing request. */
    fun cancel(subject: String): DeletionRequest?
    /** Marks an existing request as completed. */
    fun complete(subject: String): DeletionRequest?
}
