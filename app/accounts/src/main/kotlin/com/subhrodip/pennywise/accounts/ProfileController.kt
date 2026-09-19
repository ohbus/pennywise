package com.subhrodip.pennywise.accounts

import com.subhrodip.pennywise.accounts.requests.deletion.DeletionRequestService
import com.subhrodip.pennywise.ids.ApiEndpoints
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.http.ResponseEntity
import com.subhrodip.pennywise.errors.ApiProblem
import org.springframework.http.MediaType
import org.springframework.http.HttpStatusCode
import com.subhrodip.pennywise.errors.FieldViolation
import com.subhrodip.pennywise.errors.RequestIdContext
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import com.subhrodip.pennywise.errors.ApplicationException
import com.subhrodip.pennywise.errors.ErrorCode
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
            throw ApplicationException(ErrorCode.ERR_02, "At least one profile field is required")
        }
    }
}
@RestController
@RequestMapping(ApiEndpoints.Accounts.V1.BASE)
class ProfileController(
    private val profiles: ProfileStore,
    private val deletionService: DeletionRequestService,
    private val exportService: ExportRequestService
) {
    private val serviceName: String = "accounts"

    @GetMapping(ApiEndpoints.Accounts.V1.ME)
    fun get(@AuthenticationPrincipal principal: Principal): ProfileResponse =
        profiles.get(principal.name)

    @PatchMapping(ApiEndpoints.Accounts.V1.ME)
    fun update(
        @AuthenticationPrincipal principal: Principal,
        @Valid @RequestBody request: ProfilePatchRequest
    ): ProfileResponse {
        request.validateNotEmpty()
        return profiles.update(principal.name, request)
    }

    private fun problem(
        code: ErrorCode,
        status: HttpStatusCode,
        title: String = "Internal server error",
        detail: String = "An unexpected error occurred",
        violations: List<FieldViolation> = emptyList()
    ): ResponseEntity<ApiProblem> =
        ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(
                ApiProblem(
                    type = "https://pennywise.example/problems/${code.name.lowercase()}",
                    title = title,
                    status = status.value(),
                    code = mapErrorCode(code),
                    source = serviceName,
                    requestId = RequestIdContext.get(),
                    detail = detail,
                    violations = violations
                )
            )

    /**
     * Map internal ErrorCode enum to external string identifier expected by API clients/tests.
     */
    private fun mapErrorCode(errorCode: ErrorCode): String =
        when (errorCode) {
            ErrorCode.ERR_02 -> "VALIDATION_FAILED"
            ErrorCode.ERR_03 -> "UNAUTHENTICATED"
            ErrorCode.ERR_05 -> "NOT_FOUND"
            else -> errorCode.name
        }

    @PostMapping(ApiEndpoints.Accounts.V1.ME_DELETION_REQUEST)
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun requestDeletion(@AuthenticationPrincipal principal: Principal) {
        deletionService.request(principal.name)
    }

    @PostMapping(ApiEndpoints.Accounts.V1.ME_EXPORT_REQUEST)
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun requestExport(@AuthenticationPrincipal principal: Principal?): ExportRequestResponse {
        val subject = principal?.name ?: throw ApplicationException(ErrorCode.ERR_03, "Authenticated subject is required")
        val request = exportService.request(subject)
        return ExportRequestResponse(request.exportId, request.status, request.requestedAt)
    }

    @GetMapping(ApiEndpoints.Accounts.V1.ME_EXPORT_REQUESTS)
    fun listExportRequests(@AuthenticationPrincipal principal: Principal?): List<ExportRequestResponse> {
        val subject = principal?.name ?: throw ApplicationException(ErrorCode.ERR_03, "Authenticated subject is required")
        return exportService.listBySubject(subject).map {
            ExportRequestResponse(it.exportId, it.status, it.requestedAt)
        }
    }

    @GetMapping(ApiEndpoints.Accounts.V1.PROFILES_BY_ID)
    fun getProfileById(@PathVariable accountId: UUID): ProfileResponse =
        profiles.findById(accountId) ?: throw ApplicationException(ErrorCode.ERR_05, "Profile not found")

    @PostMapping(ApiEndpoints.Accounts.V1.PROFILES_BATCH)
    fun getProfilesBatch(@Valid @RequestBody request: BatchProfileRequest): List<ProfileResponse> =
        profiles.findByIds(request.accountIds)
}
