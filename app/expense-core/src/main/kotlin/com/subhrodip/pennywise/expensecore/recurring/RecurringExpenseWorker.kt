package com.subhrodip.pennywise.expensecore.recurring

import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Scheduled background worker that periodically polls for due recurring expense schedules
 * and executes bounded catch-up occurrence generation.
 */
@Component
class RecurringExpenseWorker(
    private val service: RecurringExpenseService,
    @Value("\${pennywise.recurring.enabled:false}") var enabled: Boolean = false,
    @Value("\${pennywise.recurring.max-catch-up-occurrences:12}") var maxCatchUpOccurrences: Int = 12
) {
    @Scheduled(fixedDelayString = "\${pennywise.recurring.poll-delay-ms:60000}")
    fun run(): Int {
        if (!enabled) {
            return 0
        }
        return service.processDueOccurrences(LocalDate.now(), maxCatchUpOccurrences)
    }
}
