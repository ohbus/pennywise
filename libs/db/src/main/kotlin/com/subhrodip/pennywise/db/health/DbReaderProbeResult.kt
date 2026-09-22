package com.subhrodip.pennywise.db.health

import com.subhrodip.pennywise.db.routing.DbWatermark

/** One reader health sample. */
data class DbReaderProbeResult(val lagMs: Long?, val replayedWatermark: DbWatermark?)
