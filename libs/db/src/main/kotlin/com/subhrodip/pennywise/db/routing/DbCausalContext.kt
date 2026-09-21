package com.subhrodip.pennywise.db.routing

/** Thread-bound causal requirement inherited by nested repository contexts. */
object DbCausalContext {
    private val required = ThreadLocal<String?>()

    /** Returns the inbound required watermark, if one was supplied. */
    fun requiredWatermark(): String? = required.get()

    /** Runs [block] with a validated required watermark and restores prior state. */
    fun <T> withRequiredWatermark(watermark: String?, block: () -> T): T {
        val previous = required.get()
        if (watermark == null) required.remove() else required.set(watermark)
        return try { block() } finally {
            if (previous == null) required.remove() else required.set(previous)
        }
    }
}
