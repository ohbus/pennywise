package com.subhrodip.pennywise.bff.transport.model.upstream

import com.subhrodip.pennywise.bff.transport.model.output.BffGroup
/** Group response shape received from Expense Core. */
internal data class UpstreamGroup(
    val groupId: String,
    val name: String,
    val kind: String? = null,
    val status: String? = null,
    val revision: Long = 0
) {
    fun toBffGroup(): BffGroup = BffGroup(groupId, name, kind, status, revision)
}
