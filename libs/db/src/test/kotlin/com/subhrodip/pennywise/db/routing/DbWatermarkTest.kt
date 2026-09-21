package com.subhrodip.pennywise.db.routing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class DbWatermarkTest {
    @Test
    fun `round trips postgres lsn`() {
        val watermark = DbWatermark.parse("0/16B6C50")
        assertEquals("0/16B6C50", watermark.asLsn())
        assertEquals(watermark, DbWatermark.parse(watermark.asLsn()))
    }

    @Test
    fun `rejects malformed lsn`() {
        assertFails { DbWatermark.parse("not-an-lsn") }
    }
}
