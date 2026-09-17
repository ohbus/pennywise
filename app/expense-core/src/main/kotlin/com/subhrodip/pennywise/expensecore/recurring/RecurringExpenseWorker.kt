package com.subhrodip.pennywise.expensecore.recurring

import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RecurringExpenseWorker(
    private val service: RecurringExpenseService,
    @Value("\${pennywise.recurring.enabled:false}") var enabled: Boolean = false
) {
    @Scheduled(fixedDelayString = "\${pennywise.recurring.poll-delay-ms:60000}")
    fun run(): Int {
        if (!enabled) {
            return 0
        }
        return service.processDueOccurrences(LocalDate.now())
    }
}
