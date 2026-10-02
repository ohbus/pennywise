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
            "/expense-core/v1/groups/group-1/schedules/schedule-1/pause",
            ApiEndpoints.ExpenseCore.V1.groupSchedulePause("group-1", "schedule-1")
        )
        assertEquals(
            "/notifications/v1/inbox/notification-1/read",
            ApiEndpoints.Notifications.V1.inboxMarkRead("notification-1")
        )
    }
}
