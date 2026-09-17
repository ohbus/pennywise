package com.subhrodip.pennywise.ids

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class UuidGeneratorTest {
    @Test
    fun `generates RFC 9562 version seven UUIDs`() {
        val first = UuidGenerator.next()
        val second = UuidGenerator.next()

        assertEquals(7, first.version())
        assertEquals(2, first.variant())
        assertEquals(7, second.version())
        assertNotEquals(first, second)
    }
}
