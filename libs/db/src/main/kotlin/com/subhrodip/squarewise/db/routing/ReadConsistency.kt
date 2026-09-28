package com.subhrodip.squarewise.db.routing

/** Consistency guarantee required by a query. */
enum class ReadConsistency { STRONG, SESSION, BOUNDED_STALENESS, EVENTUAL }
