package com.subhrodip.squarewise.db.health

/** Result of applying reader health to a query route. */
enum class DbReaderDecision { Reader, Writer, Fail }
