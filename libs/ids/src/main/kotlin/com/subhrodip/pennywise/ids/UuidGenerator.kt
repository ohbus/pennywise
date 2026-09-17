package com.subhrodip.pennywise.ids

import com.github.f4b6a3.uuid.UuidCreator
import java.util.UUID

/** Creates time-ordered RFC 9562 UUIDv7 identifiers for new Pennywise records. */
object UuidGenerator {
    fun next(): UUID = UuidCreator.getTimeOrderedEpoch()
}
