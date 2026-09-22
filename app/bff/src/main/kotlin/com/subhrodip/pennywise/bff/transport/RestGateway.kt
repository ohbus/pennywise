package com.subhrodip.pennywise.bff.transport

import com.subhrodip.pennywise.db.routing.DbWatermark

/** Forwards the greatest downstream writer watermark observed by the BFF. */
internal fun greatestWriterWatermark(current: String?, downstream: String?): String? {
    val candidate = downstream?.let { runCatching { DbWatermark.parse(it) }.getOrNull() } ?: return current
    val existing = current?.let { runCatching { DbWatermark.parse(it) }.getOrNull() }
    return if (existing == null || candidate > existing) candidate.asLsn() else current
}
