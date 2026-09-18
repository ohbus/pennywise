package com.subhrodip.pennywise.expensecore.settlements

import com.subhrodip.pennywise.ids.UuidGenerator
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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

import com.subhrodip.pennywise.ids.ApiEndpoints

data class RecordSettlementRequest(val fromParticipantId: UUID, val toParticipantId: UUID, @field:Pattern(regexp = "^[0-9]+$") val amountMinor: String)
data class ReverseSettlementRequest(@field:NotBlank @field:Size(max = 240) val reason: String)

@RestController
@RequestMapping(ApiEndpoints.ExpenseCore.V1.PATH_GROUP_SETTLEMENTS)
class SettlementController(
    private val service: SettlementService,
    private val suggestionEngine: SettlementSuggestionEngine? = null
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun record(@PathVariable groupId: UUID, @Valid @RequestBody request: RecordSettlementRequest): Settlement =
        service.record(groupId, UuidGenerator.next(), request.fromParticipantId, request.toParticipantId, request.amountMinor.toLong())

    @PostMapping(ApiEndpoints.ExpenseCore.V1.SETTLEMENT_REVERSAL_RELATIVE_SUBPATH)
    fun reverse(@PathVariable groupId: UUID, @PathVariable settlementId: UUID, @Valid @RequestBody request: ReverseSettlementRequest): Settlement =
        service.reverse(groupId, settlementId, request.reason)

    @GetMapping(ApiEndpoints.ExpenseCore.V1.SETTLEMENT_SUGGESTIONS_RELATIVE_SUBPATH)
    fun getSuggestions(@PathVariable groupId: UUID): List<SuggestedSettlement> =
        suggestionEngine?.suggestSettlements(groupId) ?: service.suggestions(groupId)
}
