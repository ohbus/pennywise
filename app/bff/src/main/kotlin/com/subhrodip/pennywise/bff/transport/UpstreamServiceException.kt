package com.subhrodip.pennywise.bff.transport

/** Signals a non-success response from an upstream service. */
class UpstreamServiceException(
    val status: Int,
    message: String = "Upstream service returned HTTP $status"
) : RuntimeException(message)
