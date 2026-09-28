package com.subhrodip.squarewise.db.health

import com.subhrodip.squarewise.db.routing.DbWatermark

/** One reader health sample. */
data class DbReaderProbeResult(val lagMs: Long?, val replayedWatermark: DbWatermark?)
