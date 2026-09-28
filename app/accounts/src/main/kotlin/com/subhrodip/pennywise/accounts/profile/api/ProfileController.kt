package com.subhrodip.pennywise.accounts.profile.api

import com.subhrodip.pennywise.accounts.requests.deletion.service.DeletionRequestService
import com.subhrodip.pennywise.accounts.requests.export.service.ExportRequestService
import com.subhrodip.pennywise.ids.contracts.ApiEndpoints
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.http.ResponseEntity
import com.subhrodip.pennywise.errors.http.ApiProblem
import org.springframework.http.MediaType
import org.springframework.http.HttpStatusCode
import com.subhrodip.pennywise.errors.http.FieldViolation
import com.subhrodip.pennywise.errors.request.RequestIdContext
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import com.subhrodip.pennywise.errors.domain.ApplicationException
import com.subhrodip.pennywise.errors.domain.ErrorCode
import java.security.Principal
import java.util.UUID
import com.subhrodip.pennywise.db.routing.DbContextHolder
import com.subhrodip.pennywise.db.routing.DbExecutionContext
import com.subhrodip.pennywise.db.routing.DbOperationKind
import com.subhrodip.pennywise.db.routing.ReadConsistency
import com.subhrodip.pennywise.observability.db.DbTelemetry
import com.subhrodip.pennywise.accounts.profile.persistence.ProfileStore

@RestController
@RequestMapping(ApiEndpoints.Accounts.V1.BASE)
class ProfileController(
    private val profiles: ProfileStore,
    private val deletionService: DeletionRequestService,
    private val exportService: ExportRequestService,
    private val dbTelemetry: DbTelemetry = DbTelemetry()
) {
    private val serviceName: String = "accounts"

    @GetMapping(ApiEndpoints.Accounts.V1.ME)
    fun get(principal: Principal): ProfileResponse =
        profiles.get(principal.name)

    @PatchMapping(ApiEndpoints.Accounts.V1.ME)
    fun update(
        principal: Principal,
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
    fun requestDeletion(principal: Principal) {
        deletionService.request(principal.name)
    }

    @PostMapping(ApiEndpoints.Accounts.V1.ME_EXPORT_REQUEST)
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun requestExport(principal: Principal?): ExportRequestResponse {
        val subject = principal?.name ?: throw ApplicationException(ErrorCode.ERR_03, "Authenticated subject is required")
        val request = exportService.request(subject)
        return ExportRequestResponse(request.exportId, request.status, request.requestedAt)
    }

    @GetMapping(ApiEndpoints.Accounts.V1.ME_EXPORT_REQUESTS)
    fun listExportRequests(principal: Principal?): List<ExportRequestResponse> {
        val subject = principal?.name ?: throw ApplicationException(ErrorCode.ERR_03, "Authenticated subject is required")
        return exportService.listBySubject(subject).map {
            ExportRequestResponse(it.exportId, it.status, it.requestedAt)
        }
    }

    @GetMapping(ApiEndpoints.Accounts.V1.PROFILES_BY_ID)
    fun getProfileById(@PathVariable accountId: UUID): ProfileResponse =
        dbTelemetry.measureQuery("profile.lookup", "approved-query") {
            DbContextHolder.withContext(profileReadContext("profile.lookup")) {
                profiles.findById(accountId) ?: throw ApplicationException(ErrorCode.ERR_05, "Profile not found")
            }
        }

    @PostMapping(ApiEndpoints.Accounts.V1.PROFILES_BATCH)
    fun getProfilesBatch(@Valid @RequestBody request: BatchProfileRequest): List<ProfileResponse> =
        dbTelemetry.measureQuery("profile.batch_lookup", "approved-query") {
            DbContextHolder.withContext(profileReadContext("profile.batch_lookup")) { profiles.findByIds(request.accountIds) }
        }

    private fun profileReadContext(operationName: String) = DbExecutionContext(
        operationName = operationName,
        kind = DbOperationKind.QUERY,
        consistency = ReadConsistency.EVENTUAL,
        readerEligible = true
    )
}
