package com.subhrodip.pennywise.expensecore.recurring

import com.subhrodip.pennywise.expensecore.expenses.AllocationCalculator
import com.subhrodip.pennywise.expensecore.expenses.ExpenseAllocation
import com.subhrodip.pennywise.expensecore.expenses.ExpensePayer
import com.subhrodip.pennywise.expensecore.expenses.ExpenseRecord
import com.subhrodip.pennywise.expensecore.expenses.ExpenseStore
import com.subhrodip.pennywise.expensecore.groups.GroupMembershipEntity
import com.subhrodip.pennywise.expensecore.groups.GroupRepository
import com.subhrodip.pennywise.ids.UuidGenerator
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class CreateRecurringScheduleRequest(
    val scheduleId: UUID? = null,
    val description: String,
    val amountMinor: Long,
    val currency: String,
    val frequency: RecurrenceFrequency,
    val dayOfMonth: Int? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val payers: List<ExpensePayer>? = null,
    val allocations: List<ExpenseAllocation>? = null
)

@Service
class RecurringExpenseService(
    private val scheduleRepository: RecurringExpenseScheduleRepository,
    private val occurrenceRepository: RecurringExpenseOccurrenceRepository,
    private val expenseStore: ExpenseStore,
    private val groupRepository: GroupRepository,
    @PersistenceContext private val entityManager: EntityManager
) {
    private val customSpecifications = ConcurrentHashMap<UUID, Pair<List<ExpensePayer>, List<ExpenseAllocation>>>()

    @Transactional
    fun createSchedule(groupId: UUID, request: CreateRecurringScheduleRequest): RecurringExpenseSchedule {
        require(request.description.isNotBlank()) { "description must not be blank" }
        require(request.amountMinor > 0) { "amountMinor must be positive" }
        require(request.currency.matches(Regex("^[A-Z]{3}$"))) { "currency must be 3 uppercase letters" }
        require(request.dayOfMonth == null || request.dayOfMonth in 1..31) {
            "dayOfMonth must be between 1 and 31"
        }
        require(request.frequency == RecurrenceFrequency.MONTHLY || request.dayOfMonth == null) {
            "dayOfMonth is only supported for monthly schedules"
        }
        if (request.endDate != null) {
            require(!request.endDate.isBefore(request.startDate)) {
                "endDate must not be before startDate"
            }
        }
        if (request.payers != null) {
            require(request.payers.isNotEmpty()) { "payers must not be empty if provided" }
            require(request.payers.sumOf { it.amountMinor } == request.amountMinor) {
                "sum of payer amounts must equal schedule amount"
            }
        }
        if (request.allocations != null) {
            require(request.allocations.isNotEmpty()) { "allocations must not be empty if provided" }
            require(request.allocations.sumOf { it.allocatedMinor } == request.amountMinor) {
                "sum of allocation amounts must equal schedule amount"
            }
        }

        if (!groupRepository.existsById(groupId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Group $groupId not found")
        }

        val scheduleId = request.scheduleId ?: UuidGenerator.next()
        val schedule = RecurringExpenseSchedule(
            scheduleId = scheduleId,
            groupId = groupId,
            description = request.description.trim(),
            amountMinor = request.amountMinor,
            currency = request.currency.uppercase(),
            frequency = request.frequency,
            dayOfMonth = request.dayOfMonth,
            startDate = request.startDate,
            endDate = request.endDate,
            nextOccurrenceDate = request.startDate,
            paused = false,
            createdAt = Instant.now(),
            version = 1
        )

        if (request.payers != null || request.allocations != null) {
            customSpecifications[scheduleId] = (request.payers ?: emptyList()) to (request.allocations ?: emptyList())
        }

        return scheduleRepository.save(schedule)
    }

    @Transactional
    fun pauseSchedule(scheduleId: UUID): RecurringExpenseSchedule {
        val schedule = scheduleRepository.findById(scheduleId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule $scheduleId not found")
        }
        schedule.paused = true
        return scheduleRepository.save(schedule)
    }

    @Transactional
    fun resumeSchedule(scheduleId: UUID): RecurringExpenseSchedule {
        val schedule = scheduleRepository.findById(scheduleId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule $scheduleId not found")
        }
        schedule.paused = false
        return scheduleRepository.save(schedule)
    }

    @Transactional
    fun processDueOccurrences(asOfDate: LocalDate = LocalDate.now()): Int {
        val dueSchedules = scheduleRepository.findDueSchedules(asOfDate)
        var count = 0
        for (schedule in dueSchedules) {
            count += processScheduleOccurrences(schedule, asOfDate)
        }
        return count
    }

    @Transactional(readOnly = true)
    fun getSchedule(scheduleId: UUID): RecurringExpenseSchedule? =
        scheduleRepository.findById(scheduleId).orElse(null)

    @Transactional(readOnly = true)
    fun listSchedules(groupId: UUID): List<RecurringExpenseSchedule> =
        scheduleRepository.findByGroupId(groupId)

    @Transactional(readOnly = true)
    fun getOccurrences(scheduleId: UUID): List<RecurringExpenseOccurrence> =
        occurrenceRepository.findByScheduleId(scheduleId)

    private fun processScheduleOccurrences(schedule: RecurringExpenseSchedule, asOfDate: LocalDate): Int {
        var generated = 0
        while (!schedule.paused && !schedule.nextOccurrenceDate.isAfter(asOfDate)) {
            if (schedule.endDate != null && schedule.nextOccurrenceDate.isAfter(schedule.endDate)) {
                break
            }
            val occurrenceDate = schedule.nextOccurrenceDate
            val occurrenceId = OccurrenceIdentity.id(schedule.scheduleId, occurrenceDate)

            val alreadyExists = occurrenceRepository.existsById(occurrenceId) ||
                occurrenceRepository.existsByScheduleIdAndOccurrenceDate(schedule.scheduleId, occurrenceDate)

            if (!alreadyExists) {
                val expenseRecord = buildExpenseRecord(schedule, occurrenceDate, occurrenceId)
                expenseStore.create(schedule.groupId, expenseRecord, occurrenceId.toString())

                val occurrence = RecurringExpenseOccurrence(
                    occurrenceId = occurrenceId,
                    scheduleId = schedule.scheduleId,
                    occurrenceDate = occurrenceDate,
                    expenseId = expenseRecord.expenseId,
                    createdAt = Instant.now()
                )
                occurrenceRepository.save(occurrence)
                generated++
            }

            val nextDate = RecurrencePolicy.nextAfter(occurrenceDate, schedule)
            schedule.nextOccurrenceDate = nextDate
            scheduleRepository.save(schedule)
        }
        return generated
    }

    private fun buildExpenseRecord(
        schedule: RecurringExpenseSchedule,
        occurrenceDate: LocalDate,
        occurrenceId: UUID
    ): ExpenseRecord {
        val custom = customSpecifications[schedule.scheduleId]
        val (payers, allocations) = if (custom != null && (custom.first.isNotEmpty() || custom.second.isNotEmpty())) {
            val customPayers = custom.first.ifEmpty {
                val members = getGroupMembers(schedule.groupId)
                val payerId = members.firstOrNull() ?: schedule.groupId
                listOf(ExpensePayer(payerId, schedule.amountMinor))
            }
            val customAllocations = custom.second.ifEmpty {
                val members = getGroupMembers(schedule.groupId)
                splitEqually(schedule.amountMinor, members.ifEmpty { listOf(schedule.groupId) })
            }
            customPayers to customAllocations
        } else {
            val members = getGroupMembers(schedule.groupId)
            val participantIds = members.ifEmpty { listOf(schedule.groupId) }
            val payers = listOf(ExpensePayer(participantIds.first(), schedule.amountMinor))
            val allocations = splitEqually(schedule.amountMinor, participantIds)
            payers to allocations
        }

        return ExpenseRecord(
            expenseId = occurrenceId,
            groupId = schedule.groupId,
            description = schedule.description,
            category = "recurring",
            currency = schedule.currency,
            amountMinor = schedule.amountMinor,
            version = 1,
            allocationMode = "EQUAL",
            createdAt = Instant.now(),
            payers = payers,
            allocations = allocations
        )
    }

    private fun splitEqually(amountMinor: Long, participantIds: List<UUID>): List<ExpenseAllocation> {
        val equalMap = AllocationCalculator.equal(amountMinor, participantIds.map { it.toString() })
        return equalMap.map { (pidStr, minor) ->
            ExpenseAllocation(UUID.fromString(pidStr), minor)
        }
    }

    private fun getGroupMembers(groupId: UUID): List<UUID> {
        val rows = entityManager.createQuery(
            "SELECT m.subject, m.membershipId FROM GroupMembershipEntity m WHERE m.groupId = :groupId ORDER BY m.membershipId ASC",
            Array<Any>::class.java
        ).setParameter("groupId", groupId).resultList

        return rows.map { row ->
            val subject = row[0] as String
            val membershipId = row[1] as UUID
            try {
                UUID.fromString(subject)
            } catch (_: IllegalArgumentException) {
                UUID.nameUUIDFromBytes(subject.toByteArray(StandardCharsets.UTF_8))
            }
        }
    }
}
