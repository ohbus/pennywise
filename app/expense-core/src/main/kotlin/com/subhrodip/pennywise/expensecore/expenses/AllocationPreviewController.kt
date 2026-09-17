package com.subhrodip.pennywise.expensecore.expenses

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class AllocationPreviewRequest(
    @field:NotBlank @field:Pattern(regexp = "^[0-9]+$") val totalMinor: String,
    @field:NotEmpty @field:Size(max = 100) val participantIds: List<@NotBlank String>
)
data class AllocationPreviewResponse(val totalMinor: String, val allocations: Map<String, String>)

@RestController
@RequestMapping("/expense-core/v1/allocations")
class AllocationPreviewController {
    @PostMapping("/preview")
    @ResponseStatus(HttpStatus.OK)
    fun preview(@Valid @RequestBody request: AllocationPreviewRequest): AllocationPreviewResponse {
        val total = request.totalMinor.toLongOrNull()
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "totalMinor must be a non-negative integer")
        if (total < 0) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "totalMinor must be non-negative")
        val allocations = try {
            AllocationCalculator.equal(total, request.participantIds)
        } catch (error: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, error.message, error)
        }
            .mapValues { (_, value) -> value.toString() }
        return AllocationPreviewResponse(request.totalMinor, allocations)
    }
}
