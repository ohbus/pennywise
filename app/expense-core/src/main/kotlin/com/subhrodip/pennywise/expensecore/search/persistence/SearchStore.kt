package com.subhrodip.pennywise.expensecore.search.persistence

import com.subhrodip.pennywise.expensecore.search.api.SearchQuery
import com.subhrodip.pennywise.expensecore.search.model.SearchExpense
import java.nio.charset.StandardCharsets
import java.util.Base64
import com.subhrodip.pennywise.errors.domain.ApplicationException
import com.subhrodip.pennywise.errors.domain.ErrorCode

/**
 * Domain port for durable search and retrieval of expenses within an authorized group.
 */
interface SearchStore {
    /**
     * Finds all active (non-deleted) expenses belonging to the specified group.
     *
     * @param groupId the UUID of the group
     * @return sequence or list of domain [SearchExpense] records
     */
    fun findSearchExpenses(query: SearchQuery): List<SearchExpense>
}

/** Normalized bounded search request passed to the read adapter. */
/** Decodes the public opaque cursor before it reaches SQL or an in-memory adapter. */
fun decodeSearchCursor(cursor: String?): String? = cursor?.let {
    runCatching {
        String(Base64.getUrlDecoder().decode(it), StandardCharsets.UTF_8).also { value -> require(value.isNotBlank()) }
    }.getOrElse { throw ApplicationException(ErrorCode.ERR_02, "Invalid search cursor", it) }
}
