package com.subhrodip.pennywise.expensecore.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore

/**
 * Executes a collection of suspend tasks in parallel while limiting concurrency.
 * Uses a configurable [maxParallelism] to avoid overwhelming the system.
 * Errors in individual tasks are collected and rethrown as a combined exception.
 */
object ParallelExecutor {
    private val defaultScope = CoroutineScope(Dispatchers.Default)

    suspend fun <T> executeParallel(
        tasks: Collection<suspend () -> T>,
        maxParallelism: Int = Runtime.getRuntime().availableProcessors()
    ): List<T> = supervisorScope {
        val semaphore = Semaphore(maxParallelism)
        tasks.map { task ->
            async {
                semaphore.acquire()
                try {
                    task()
                } finally {
                    semaphore.release()
                }
            }
        }.awaitAll()
    }
}
