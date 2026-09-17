package com.subhrodip.pennywise.accounts

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.nio.charset.StandardCharsets
import java.security.Principal
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class ExportRequestResponse(
    val exportId: UUID,
    val status: ExportStatus,
    val requestedAt: Instant
)

data class ProfileResponse(
    val accountId: UUID,
    val displayName: String,
    val timezone: String,
    val defaultCurrency: String
)

data class BatchProfileRequest(
    @field:Size(min = 1, max = 100) val accountIds: List<UUID>
)

data class ProfilePatchRequest(
    @field:NotBlank @field:Size(max = 120) val displayName: String? = null,
    @field:NotBlank @field:Size(max = 80) val timezone: String? = null,
    @field:Pattern(regexp = "^[A-Z]{3}$") val defaultCurrency: String? = null
) {
    fun validateNotEmpty() {
        if (displayName == null && timezone == null && defaultCurrency == null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one profile field is required")
        }
    }
}

@RestController
@RequestMapping("/accounts/v1")
class ProfileController(
    private val profiles: ProfileStore,
    private val deletionService: DeletionRequestService,
    private val exportService: ExportRequestService
) {
    @GetMapping("/me")
    fun get(@AuthenticationPrincipal principal: Principal): ProfileResponse =
        profiles.get(principal.name)

    @PatchMapping("/me")
    fun update(
        @AuthenticationPrincipal principal: Principal,
        @Valid @RequestBody request: ProfilePatchRequest
    ): ProfileResponse {
        request.validateNotEmpty()
        return profiles.update(principal.name, request)
    }

    @PostMapping("/me/deletion-request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun requestDeletion(@AuthenticationPrincipal principal: Principal) {
        deletionService.request(principal.name)
    }

    @PostMapping("/me/export-request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun requestExport(@AuthenticationPrincipal principal: Principal?): ExportRequestResponse {
        val subject = principal?.name ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated subject is required")
        val request = exportService.request(subject)
        return ExportRequestResponse(request.exportId, request.status, request.requestedAt)
    }

    @GetMapping("/me/export-requests")
    fun listExportRequests(@AuthenticationPrincipal principal: Principal?): List<ExportRequestResponse> {
        val subject = principal?.name ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated subject is required")
        return exportService.listBySubject(subject).map {
            ExportRequestResponse(it.exportId, it.status, it.requestedAt)
        }
    }

    @GetMapping("/profiles/{accountId}")
    fun getProfileById(@PathVariable accountId: UUID): ProfileResponse =
        profiles.findById(accountId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found")

    @PostMapping("/profiles/batch")
    fun getProfilesBatch(@Valid @RequestBody request: BatchProfileRequest): List<ProfileResponse> =
        profiles.findByIds(request.accountIds)
}
