package com.subhrodip.pennywise.db.routing

/** Request-scoped database execution metadata used by adapters and telemetry. */
data class DbExecutionContext(
    /** Stable operation identity. */
    val operationName: String,
    /** Operation safety classification. */
    val kind: DbOperationKind,
    /** Required consistency guarantee. */
    val consistency: ReadConsistency = ReadConsistency.STRONG,
    /** Whether the named operation has been approved for replica execution. */
    val readerEligible: Boolean = false,
    /** Optional writer watermark required before a replica may answer. */
    val requiredWatermark: String? = null
) {
    init {
        require(operationName.matches(OPERATION_NAME)) {
            "operationName must be a stable lowercase dot-delimited identifier"
        }
    }

    /** Returns whether this context is intrinsically forbidden from a reader. */
    fun isWriterOnly(): Boolean = kind != DbOperationKind.QUERY || consistency == ReadConsistency.STRONG || !readerEligible

    private companion object {
        val OPERATION_NAME = Regex("[a-z0-9]+(?:[._-][a-z0-9]+)*")
    }
}
