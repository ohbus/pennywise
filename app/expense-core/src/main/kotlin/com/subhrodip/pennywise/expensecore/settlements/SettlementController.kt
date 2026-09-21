package com.subhrodip.pennywise.expensecore.settlements

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import java.security.Principal
import com.subhrodip.pennywise.expensecore.groups.GroupMembershipRepository
import com.subhrodip.pennywise.errors.ApplicationException
import com.subhrodip.pennywise.errors.ErrorCode

import com.subhrodip.pennywise.ids.ApiEndpoints

data class RecordSettlementRequest(val fromParticipantId: UUID, val toParticipantId: UUID, @field:Pattern(regexp = "^[0-9]+$") val amountMinor: String)
data class ReverseSettlementRequest(@field:NotBlank @field:Size(max = 240) val reason: String)

@RestController
@RequestMapping(ApiEndpoints.ExpenseCore.V1.PATH_GROUP_SETTLEMENTS)
class SettlementController(
    private val service: SettlementService,
    private val membershipRepository: GroupMembershipRepository,
    private val suggestionEngine: SettlementSuggestionEngine? = null
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun record(@PathVariable groupId: UUID, @RequestHeader(ApiEndpoints.Headers.IDEMPOTENCY_KEY) idempotencyKey: String, @Valid @RequestBody request: RecordSettlementRequest, principal: Principal?): Settlement {
        ensureMembership(groupId, principal)
        return service.record(groupId, UUID.randomUUID(), request.fromParticipantId, request.toParticipantId, request.amountMinor.toLong(), principal!!.name, idempotencyKey)
    }

    @PostMapping(ApiEndpoints.ExpenseCore.V1.SETTLEMENT_REVERSAL_RELATIVE_SUBPATH)
    fun reverse(@PathVariable groupId: UUID, @PathVariable settlementId: UUID, @Valid @RequestBody request: ReverseSettlementRequest, principal: Principal?): Settlement {
        ensureMembership(groupId, principal)
        return service.reverse(groupId, settlementId, request.reason)
    }

    @GetMapping(ApiEndpoints.ExpenseCore.V1.SETTLEMENT_SUGGESTIONS_RELATIVE_SUBPATH)
    fun getSuggestions(@PathVariable groupId: UUID, principal: Principal?): List<SuggestedSettlement> {
        ensureMembership(groupId, principal)
        return suggestionEngine?.suggestSettlements(groupId) ?: service.suggestions(groupId)
    }

    private fun ensureMembership(groupId: UUID, principal: Principal?) {
        val subject = principal?.name?.takeIf { it.isNotBlank() }
            ?: throw ApplicationException(ErrorCode.ERR_03, "Authenticated subject is required")
        if (!membershipRepository.existsByGroupIdAndSubject(groupId, subject)) {
            throw ApplicationException(ErrorCode.ERR_05, "Group $groupId not found")
        }
    }
}
