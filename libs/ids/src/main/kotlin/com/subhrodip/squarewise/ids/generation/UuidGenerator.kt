package com.subhrodip.squarewise.ids.generation

import com.github.f4b6a3.uuid.UuidCreator
import java.util.UUID

/** Creates time-ordered RFC 9562 UUIDv7 identifiers for new Squarewise records. */
object UuidGenerator {
    fun next(): UUID = UuidCreator.getTimeOrderedEpoch()
}
