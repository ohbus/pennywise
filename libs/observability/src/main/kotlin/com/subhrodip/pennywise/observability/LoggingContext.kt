package com.subhrodip.pennywise.observability

import org.slf4j.MDC

/** Executes background work with a scoped correlation value and reliable MDC cleanup. */
object LoggingContext {
    /**
     * Runs [action] with [value] bound to [key], restoring the previous value afterward.
     *
     * @param key MDC key to bind
     * @param value correlation value
     * @param action scoped work
     */
    fun <T> with(key: String, value: String, action: () -> T): T {
        val previous = MDC.get(key)
        MDC.put(key, value)
        return try {
            action()
        } finally {
            if (previous == null) MDC.remove(key) else MDC.put(key, previous)
        }
    }
}
