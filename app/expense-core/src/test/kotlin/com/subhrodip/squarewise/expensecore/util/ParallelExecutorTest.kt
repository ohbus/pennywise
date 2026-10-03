package com.subhrodip.squarewise.expensecore.util

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ParallelExecutorTest {

    @Test
    fun `executes every task and preserves input order`() = runBlocking {
        val results = ParallelExecutor.executeParallel(
            tasks = listOf(
                { 3 },
                { 1 },
                { 2 },
            ),
            maxParallelism = 2,
        )

        assertEquals(listOf(3, 1, 2), results)
    }

    @Test
    fun `propagates a task failure to the caller`() {
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                ParallelExecutor.executeParallel(
                    tasks = listOf(
                        { throw IllegalStateException("task failed") },
                        { 42 },
                    ),
                    maxParallelism = 2,
                )
            }
        }
    }

    @Test
    fun `uses the available processor default when parallelism is omitted`() = runBlocking {
        val results = ParallelExecutor.executeParallel(
            tasks = listOf({ "first" }, { "second" })
        )

        assertEquals(listOf("first", "second"), results)
    }
}
