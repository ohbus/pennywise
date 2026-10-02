package com.subhrodip.squarewise.accounts.auth.credential

import java.time.Duration
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Scheduled background task that purges expired and consumed login credentials
 * to maintain bounded table growth and minimize stale credential retention.
 */
@Component
@ConditionalOnProperty(name = ["squarewise.auth.credential-cleanup-enabled"], havingValue = "true", matchIfMissing = true)
class CredentialCleanupTask(
    private val loginCredentialRepository: LoginCredentialRepository,
    @Value("\${squarewise.auth.credential-retention-expired-days:7}") private val expiredRetentionDays: Long,
    @Value("\${squarewise.auth.credential-retention-consumed-hours:24}") private val consumedRetentionHours: Long
) {
    private val log = LoggerFactory.getLogger(CredentialCleanupTask::class.java)

    /**
     * Purges expired credentials older than [expiredRetentionDays] days and consumed
     * credentials older than [consumedRetentionHours] hours.
     */
    @Scheduled(cron = "\${squarewise.auth.credential-cleanup-cron:0 0 * * * *}")
    @Transactional
    fun executeCleanup(): Int {
        val now = Instant.now()
        val expiredCutoff = now.minus(Duration.ofDays(expiredRetentionDays))
        val consumedCutoff = now.minus(Duration.ofHours(consumedRetentionHours))

        val deletedCount = loginCredentialRepository.purgeExpiredOrConsumed(expiredCutoff, consumedCutoff)
        if (deletedCount > 0) {
            log.info("Purged {} expired or consumed auth login credentials (expiredCutoff={}, consumedCutoff={})", deletedCount, expiredCutoff, consumedCutoff)
        }
        return deletedCount
    }
}
