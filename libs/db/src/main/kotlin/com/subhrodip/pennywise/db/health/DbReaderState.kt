package com.subhrodip.pennywise.db.health

/** Lifecycle states used to keep an unhealthy reader out of query routing. */
enum class DbReaderState { HEALTHY, LAGGING, OPEN, DISCONNECTED }
