package com.subhrodip.pennywise.bff.realtime

import com.subhrodip.pennywise.ids.generation.UuidGenerator

/** A committed group change propagated to BFF realtime subscribers. */
data class GroupInvalidation(val groupId: String, val revision: Long, val changeId: String = UuidGenerator.next().toString())
