package com.subhrodip.pennywise.ids

/**
 * Authoritative central constants for REST API paths, parameters, and headers
 * used across all Pennywise services and BFF gateways, organized by service and API version.
 */
object ApiEndpoints {

    /** Common HTTP Headers */
    object Headers {
        const val REQUEST_ID: String = "X-Request-Id"
        const val IDEMPOTENCY_KEY: String = "Idempotency-Key"
    }

    /** Accounts service API endpoints */
    object Accounts {
        /** Version 1 endpoints for Accounts service */
        object V1 {
            const val BASE: String = "/accounts/v1"
            const val ME: String = "/me"
            const val ME_DELETION_REQUEST: String = "/me/deletion-request"
            const val ME_EXPORT_REQUEST: String = "/me/export-request"
            const val ME_EXPORT_REQUESTS: String = "/me/export-requests"
            const val PROFILES_BY_ID: String = "/profiles/{accountId}"
            const val PROFILES_BATCH: String = "/profiles/batch"

            const val PATH_ME: String = "$BASE$ME"
            const val PATH_ME_DELETION_REQUEST: String = "$BASE$ME_DELETION_REQUEST"
            const val PATH_ME_EXPORT_REQUEST: String = "$BASE$ME_EXPORT_REQUEST"
            const val PATH_ME_EXPORT_REQUESTS: String = "$BASE$ME_EXPORT_REQUESTS"
            const val PATH_PROFILES_BY_ID: String = "$BASE$PROFILES_BY_ID"
            const val PATH_PROFILES_BATCH: String = "$BASE$PROFILES_BATCH"

            fun profileById(accountId: Any): String = "$BASE/profiles/$accountId"
        }
    }

    /** Expense Core service API endpoints */
    object ExpenseCore {
        /** Version 1 endpoints for Expense Core service */
        object V1 {
            const val BASE: String = "/expense-core/v1"
            const val GROUPS: String = "/groups"
            const val GROUP_BY_ID: String = "/groups/{groupId}"
            const val GROUP_MEMBERS: String = "/groups/{groupId}/members"
            const val GROUP_INVITES: String = "/groups/{groupId}/invites"
            const val INVITES: String = "/invites"
            const val INVITE_CLAIM: String = "/invites/{token}/claim"

            // Subpaths relative to /groups/{groupId}
            const val EXPENSES_SUBPATH: String = "/expenses"
            const val EXPENSE_BY_ID_SUBPATH: String = "/expenses/{expenseId}"
            const val BALANCES_SUBPATH: String = "/balances"
            const val MEMBERS_SUBPATH: String = "/members"
            const val INVITES_SUBPATH: String = "/invites"
            const val SETTLEMENTS_SUBPATH: String = "/settlements"
            const val SETTLEMENT_REVERSAL_SUBPATH: String = "/settlements/{settlementId}/reversal"
            const val SETTLEMENT_SUGGESTIONS_SUBPATH: String = "/settlements/suggestions"
            const val SETTLEMENT_REVERSAL_RELATIVE_SUBPATH: String = "/{settlementId}/reversal"
            const val SETTLEMENT_SUGGESTIONS_RELATIVE_SUBPATH: String = "/suggestions"
            const val SYNC_SUBPATH: String = "/sync"
            const val SYNC_SNAPSHOT_RELATIVE_SUBPATH: String = "/snapshot"
            const val SYNC_CHANGES_RELATIVE_SUBPATH: String = "/changes"
            const val GROUP_SYNC: String = "/groups/{groupId}$SYNC_SUBPATH"
            const val PATH_GROUP_SYNC: String = "$BASE$GROUP_SYNC"
            const val GROUP_EXPENSES: String = "/groups/{groupId}$EXPENSES_SUBPATH"
            const val GROUP_EXPENSE_BY_ID: String = "/groups/{groupId}$EXPENSE_BY_ID_SUBPATH"
            const val GROUP_BALANCES: String = "/groups/{groupId}$BALANCES_SUBPATH"

            const val GROUP_SETTLEMENTS: String = "/groups/{groupId}$SETTLEMENTS_SUBPATH"
            const val GROUP_SETTLEMENT_REVERSAL: String = "/groups/{groupId}$SETTLEMENT_REVERSAL_SUBPATH"
            const val GROUP_SETTLEMENT_SUGGESTIONS: String = "/groups/{groupId}$SETTLEMENT_SUGGESTIONS_SUBPATH"

            const val ALLOCATIONS: String = "/allocations"
            const val ALLOCATION_PREVIEW_SUBPATH: String = "/preview"
            const val PATH_ALLOCATION_PREVIEW: String = "$BASE$ALLOCATIONS$ALLOCATION_PREVIEW_SUBPATH"

            const val GROUP_SYNC_SNAPSHOT: String = "/groups/{groupId}/sync/snapshot"
            const val GROUP_SYNC_CHANGES: String = "/groups/{groupId}/sync/changes"

            // Absolute application paths starting with BASE
            const val PATH_GROUPS: String = "$BASE$GROUPS"
            const val PATH_GROUP_BY_ID: String = "$BASE$GROUP_BY_ID"
            const val PATH_GROUP_MEMBERS: String = "$BASE$GROUP_MEMBERS"
            const val PATH_GROUP_INVITES: String = "$BASE$GROUP_INVITES"
            const val PATH_INVITES: String = "$BASE$INVITES"
            const val PATH_INVITE_CLAIM: String = "$BASE$INVITE_CLAIM"
            const val PATH_GROUP_EXPENSES: String = "$BASE$GROUP_EXPENSES"
            const val PATH_GROUP_EXPENSE_BY_ID: String = "$BASE$GROUP_EXPENSE_BY_ID"
            const val PATH_GROUP_BALANCES: String = "$BASE$GROUP_BALANCES"
            const val PATH_GROUP_SETTLEMENTS: String = "$BASE$GROUP_SETTLEMENTS"
            const val PATH_GROUP_SETTLEMENT_REVERSAL: String = "$BASE$GROUP_SETTLEMENT_REVERSAL"
            const val PATH_GROUP_SETTLEMENT_SUGGESTIONS: String = "$BASE$GROUP_SETTLEMENT_SUGGESTIONS"
            const val PATH_GROUP_SYNC_SNAPSHOT: String = "$BASE$GROUP_SYNC_SNAPSHOT"
            const val PATH_GROUP_SYNC_CHANGES: String = "$BASE$GROUP_SYNC_CHANGES"

            fun groupById(groupId: Any): String = "$BASE/groups/$groupId"
            fun groupMembers(groupId: Any): String = "$BASE/groups/$groupId/members"
            fun groupInvites(groupId: Any): String = "$BASE/groups/$groupId/invites"
            fun inviteClaim(token: String): String = "$BASE/invites/$token/claim"
            fun groupExpenses(groupId: Any): String = "$BASE/groups/$groupId/expenses"
            fun groupExpenseById(groupId: Any, expenseId: Any): String = "$BASE/groups/$groupId/expenses/$expenseId"
            fun groupBalances(groupId: Any): String = "$BASE/groups/$groupId/balances"
            fun groupSettlements(groupId: Any): String = "$BASE/groups/$groupId/settlements"
            fun groupSettlementReversal(groupId: Any, settlementId: Any): String = "$BASE/groups/$groupId/settlements/$settlementId/reversal"
            fun groupSettlementSuggestions(groupId: Any): String = "$BASE/groups/$groupId/settlements/suggestions"
            fun groupSyncSnapshot(groupId: Any): String = "$BASE/groups/$groupId/sync/snapshot"
            fun groupSyncChanges(groupId: Any): String = "$BASE/groups/$groupId/sync/changes"
        }
    }

    /** Notifications service API endpoints */
    object Notifications {
        /** Version 1 endpoints for Notifications service */
        object V1 {
            const val BASE: String = "/notifications/v1"
            const val INBOX: String = "/inbox"
            const val INBOX_MARK_READ_SUBPATH: String = "/{notificationId}/read"
            const val INBOX_MARK_READ: String = "/inbox$INBOX_MARK_READ_SUBPATH"
            const val PREFERENCES: String = "/preferences"

            const val PATH_INBOX: String = "$BASE$INBOX"
            const val PATH_INBOX_MARK_READ: String = "$BASE$INBOX_MARK_READ"
            const val PATH_PREFERENCES: String = "$BASE$PREFERENCES"

            fun inboxMarkRead(notificationId: Any): String = "$BASE/inbox/$notificationId/read"
        }
    }
}
