package com.subhrodip.squarewise.db.routing

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

    @Test
    fun `rejects negative watermark positions`() {
        assertFails { DbWatermark.fromPosition(-1) }
    }

    @Test
    fun `creates a watermark from a valid postgres position`() {
        val watermark = DbWatermark.fromPosition(0x16B6C50L)

        assertEquals("0/16B6C50", watermark.asLsn())
        assertEquals(watermark, DbWatermark.parse(watermark.asLsn()))
    }

    @Test
    fun `rejects missing and oversized lsn components`() {
        assertFails { DbWatermark.parse("0/") }
        assertFails { DbWatermark.parse("100000000/1") }
        assertFails { DbWatermark.parse("1/100000000") }
    }
}
