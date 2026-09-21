package com.subhrodip.pennywise.db.routing

/** Identifies the physical database target selected for one operation. */
enum class DbRoute {
    /** The sole authoritative PostgreSQL writer. */
    WRITER,

    /** A configured read-only replica pool. */
    READER
}

/** Classifies a database operation for route-safety enforcement. */
enum class DbOperationKind {
    /** An operation that can change authoritative state. */
    COMMAND,

    /** A read that does not participate in a mutation decision. */
    QUERY,

    /** A read that acquires a lock or makes a mutation decision. */
    LOCKING_QUERY,

    /** A background claim/lease operation. */
    CLAIM,

    /** Schema management, always executed by the writer. */
    MIGRATION,

    /** Operator verification of authoritative state. */
    RECONCILIATION
}

/** Consistency guarantee required by a query. */
enum class ReadConsistency {
    /** The query must observe the latest committed writer state. */
    STRONG,

    /** The query must observe a previously returned writer watermark. */
    SESSION,

    /** Replica data may be stale within an explicitly bounded interval. */
    BOUNDED_STALENESS,

    /** Replica lag is acceptable and documented by the caller. */
    EVENTUAL
}
