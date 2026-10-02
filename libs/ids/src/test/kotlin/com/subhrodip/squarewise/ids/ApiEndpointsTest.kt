package com.subhrodip.squarewise.ids

import com.subhrodip.squarewise.ids.contracts.ApiEndpoints
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Verifies public path builders preserve the REST contract prefixes and variables. */
class ApiEndpointsTest {
    /** Checks representative parameterized paths for each independently deployed service. */
    @Test
    fun `path builders preserve service prefixes and resource identifiers`() {
        assertEquals(
            "/accounts/v1/profiles/account-1",
            ApiEndpoints.Accounts.V1.profileById("account-1")
        )
        assertEquals(
            "/expense-core/v1/groups/group-1/expenses/expense-1",
            ApiEndpoints.ExpenseCore.V1.groupExpenseById("group-1", "expense-1")
        )
        assertEquals(
            "/expense-core/v1/groups/group-1/archive",
            ApiEndpoints.ExpenseCore.V1.groupArchive("group-1")
        )
        assertEquals(
            "/expense-core/v1/groups/group-1",
            ApiEndpoints.ExpenseCore.V1.groupById("group-1")
        )
        assertEquals(
            "/expense-core/v1/groups/group-1/members",
            ApiEndpoints.ExpenseCore.V1.groupMembers("group-1")
        )
        assertEquals(
            "/expense-core/v1/groups/group-1/members/membership-1",
            ApiEndpoints.ExpenseCore.V1.groupMemberById("group-1", "membership-1")
        )
        assertEquals(
            "/expense-core/v1/groups/group-1/placeholders",
            ApiEndpoints.ExpenseCore.V1.groupPlaceholders("group-1")
        )
        assertEquals(
            "/expense-core/v1/groups/group-1/invites/token-1/revoke",
            ApiEndpoints.ExpenseCore.V1.groupInviteRevoke("group-1", "token-1")
        )
        assertEquals(
            "/expense-core/v1/groups/group-1/invites",
            ApiEndpoints.ExpenseCore.V1.groupInvites("group-1")
        )
        assertEquals(
            "/expense-core/v1/invites/token-1/claim",
            ApiEndpoints.ExpenseCore.V1.inviteClaim("token-1")
        )
        assertEquals(
            "/expense-core/v1/groups/group-1/expenses",
            ApiEndpoints.ExpenseCore.V1.groupExpenses("group-1")
        )
        assertEquals(
            "/expense-core/v1/groups/group-1/balances",
            ApiEndpoints.ExpenseCore.V1.groupBalances("group-1")
        )
        assertEquals(
            "/expense-core/v1/groups/group-1/settlements",
            ApiEndpoints.ExpenseCore.V1.groupSettlements("group-1")
        )
        assertEquals(
            "/expense-core/v1/groups/group-1/settlements/settlement-1/reversal",
            ApiEndpoints.ExpenseCore.V1.groupSettlementReversal("group-1", "settlement-1")
        )
        assertEquals(
            "/expense-core/v1/groups/group-1/settlements/suggestions",
            ApiEndpoints.ExpenseCore.V1.groupSettlementSuggestions("group-1")
        )
        assertEquals(
            "/expense-core/v1/groups/group-1/sync/snapshot",
            ApiEndpoints.ExpenseCore.V1.groupSyncSnapshot("group-1")
        )
        assertEquals(
            "/expense-core/v1/groups/group-1/sync/changes",
            ApiEndpoints.ExpenseCore.V1.groupSyncChanges("group-1")
        )
        assertEquals(
            "/expense-core/v1/groups/group-1/search",
            ApiEndpoints.ExpenseCore.V1.groupSearch("group-1")
        )
        assertEquals(
            "/expense-core/v1/groups/group-1/export",
            ApiEndpoints.ExpenseCore.V1.groupExport("group-1")
        )
        assertEquals(
            "/expense-core/v1/groups/group-1/schedules",
            ApiEndpoints.ExpenseCore.V1.groupSchedules("group-1")
        )
        assertEquals(
            "/expense-core/v1/groups/group-1/schedules/schedule-1",
            ApiEndpoints.ExpenseCore.V1.groupScheduleById("group-1", "schedule-1")
        )
        assertEquals(
            "/expense-core/v1/groups/group-1/schedules/schedule-1/pause",
            ApiEndpoints.ExpenseCore.V1.groupSchedulePause("group-1", "schedule-1")
        )
        assertEquals(
            "/expense-core/v1/groups/group-1/schedules/schedule-1/resume",
            ApiEndpoints.ExpenseCore.V1.groupScheduleResume("group-1", "schedule-1")
        )
        assertEquals(
            "/notifications/v1/inbox/notification-1/read",
            ApiEndpoints.Notifications.V1.inboxMarkRead("notification-1")
        )
    }
}
