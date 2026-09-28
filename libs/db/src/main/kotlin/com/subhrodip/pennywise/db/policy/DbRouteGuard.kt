package com.subhrodip.pennywise.db.policy

import com.subhrodip.pennywise.db.routing.DbExecutionContext
import com.subhrodip.pennywise.db.routing.DbRoute

/** Prevents unsafe operations from being routed to a read-only replica. */
class DbRouteGuard {
    /**
     * Validates a requested route against the operation context.
     *
     * @throws IllegalStateException when a reader would violate consistency or mutation safety.
     */
    fun validate(context: DbExecutionContext, requestedRoute: DbRoute) {
        if (requestedRoute == DbRoute.READER && context.isWriterOnly()) {
            throw IllegalStateException(
                "Database operation '${context.operationName}' must execute on the writer"
            )
        }
    }
}
