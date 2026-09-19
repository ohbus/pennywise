package com.subhrodip.pennywise.expensecore.search

import com.subhrodip.pennywise.expensecore.groups.GroupStore
import com.subhrodip.pennywise.ids.ApiEndpoints
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import com.subhrodip.pennywise.errors.ApplicationException
import com.subhrodip.pennywise.errors.ErrorCode
import java.security.Principal
import java.util.UUID

/**
 * Controller exposing authorized, persistent group-scoped expense search and CSV export endpoints.
 *
 * Enforces authenticated group membership before delegating search queries and safe CSV export
 * generation using [ExpenseSearch].
 */
@RestController
@RequestMapping(ApiEndpoints.ExpenseCore.V1.PATH_GROUP_BY_ID)
class SearchController(
    private val groupStore: GroupStore,
    private val searchStore: SearchStore,
    private val expenseSearch: ExpenseSearch = ExpenseSearch()
) {

    /**
     * Searches active expenses within an authorized group with filtering, pagination cursor, and currency totals.
     *
     * @param groupId the group identifier
     * @param query optional text query matching expense description case-insensitively
     * @param currency optional 3-letter currency code filter
     * @param category optional expense category filter
     * @param cursor optional pagination cursor
     * @param limit maximum number of results to return (1..1000, default 100)
     * @param principal authenticated user principal
     * @return [ExpenseSearchPage] containing matching expenses, cursor, and currency totals
     * @throws ResponseStatusException if group not found or user not a member
     */
    @GetMapping(ApiEndpoints.ExpenseCore.V1.SEARCH_SUBPATH)
    fun search(
        @PathVariable groupId: UUID,
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) currency: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "100") limit: Int,
        principal: Principal
    ): ExpenseSearchPage {
        ensureMembership(groupId, principal.name)
        val expenses = searchStore.findSearchExpenses(groupId)
        return try {
            expenseSearch.page(
                expenses = expenses,
                query = query ?: "",
                currency = currency,
                category = category,
                cursor = cursor,
                limit = limit
            )
        } catch (ex: IllegalArgumentException) {
            throw ApplicationException(ErrorCode.ERR_02, ex.message, ex)
        }
    }

    /**
     * Generates a safe CSV file export of expenses within an authorized group.
     *
     * @param groupId the group identifier
     * @param query optional text query matching expense description case-insensitively
     * @param currency optional 3-letter currency code filter
     * @param category optional expense category filter
     * @param maxRows maximum number of export rows permitted (1..10000, default 10000)
     * @param principal authenticated user principal
     * @return [ResponseEntity] containing CSV data with Content-Disposition attachment header
     * @throws ResponseStatusException if group not found, user not a member, or export exceeds limit
     */
    @GetMapping(ApiEndpoints.ExpenseCore.V1.EXPORT_SUBPATH, produces = ["text/csv; charset=UTF-8"])
    fun export(
        @PathVariable groupId: UUID,
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) currency: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(defaultValue = "10000") maxRows: Int,
        principal: Principal
    ): ResponseEntity<String> {
        ensureMembership(groupId, principal.name)
        val expenses = searchStore.findSearchExpenses(groupId)
        val csvData = try {
            expenseSearch.csv(
                expenses = expenses,
                query = query ?: "",
                currency = currency,
                category = category,
                maxRows = maxRows
            )
        } catch (ex: IllegalArgumentException) {
            throw ApplicationException(ErrorCode.ERR_02, ex.message, ex)
        }

        val filename = "expenses-$groupId.csv"
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .body(csvData)
    }

    private fun ensureMembership(groupId: UUID, subject: String) {
        val groups = groupStore.list(subject)
        if (groups.none { it.groupId == groupId }) {
            throw ApplicationException(ErrorCode.ERR_05, "Group $groupId not found")
        }
    }
}
