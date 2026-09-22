package com.subhrodip.pennywise.db.routing

/** Classifies a database operation for route-safety enforcement. */
enum class DbOperationKind { COMMAND, QUERY, LOCKING_QUERY, CLAIM, MIGRATION, RECONCILIATION }
