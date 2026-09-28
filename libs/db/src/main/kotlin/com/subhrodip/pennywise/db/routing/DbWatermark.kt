package com.subhrodip.pennywise.db.routing

/** Opaque causal position returned by PostgreSQL, represented as a monotonic LSN value. */
@JvmInline
value class DbWatermark private constructor(val position: Long) : Comparable<DbWatermark> {
    init { require(position >= 0) { "watermark position must not be negative" } }

    override fun compareTo(other: DbWatermark): Int = position.compareTo(other.position)

    /** Returns the PostgreSQL `X/XXXXXXX` textual LSN form. */
    fun asLsn(): String = "${(position ushr 32).toString(16)}/${(position and 0xffffffffL).toString(16)}".uppercase()

    companion object {
        /** Parses a PostgreSQL LSN without using wall-clock time. */
        fun parse(value: String): DbWatermark {
            val parts = value.trim().split('/')
            require(parts.size == 2) { "watermark must be a PostgreSQL LSN" }
            val high = parts[0].toULong(16)
            val low = parts[1].toULong(16)
            require(high <= UInt.MAX_VALUE.toULong() && low <= UInt.MAX_VALUE.toULong()) {
                "watermark LSN component is out of range"
            }
            return DbWatermark((high.toLong() shl 32) or low.toLong())
        }

        fun fromPosition(position: Long): DbWatermark = DbWatermark(position)
    }
}

/** Header names used when a writer watermark crosses a service boundary. */
object DbWatermarkHeaders {
    const val WRITER_WATERMARK = "X-Pennywise-Writer-Watermark"
    const val REQUIRED_WATERMARK = "X-Pennywise-Required-Watermark"
}
