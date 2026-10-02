package com.subhrodip.squarewise.accounts.profile.api
import org.springframework.web.bind.annotation.RequestHeader

import com.subhrodip.squarewise.accounts.requests.deletion.service.DeletionRequestService
import com.subhrodip.squarewise.accounts.requests.export.service.ExportRequestService
import com.subhrodip.squarewise.ids.contracts.ApiEndpoints
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.http.ResponseEntity
import com.subhrodip.squarewise.errors.http.ApiProblem
import org.springframework.http.MediaType
import org.springframework.http.HttpStatusCode
import com.subhrodip.squarewise.errors.http.FieldViolation
import com.subhrodip.squarewise.errors.request.RequestIdContext
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import com.subhrodip.squarewise.errors.domain.ApplicationException
import com.subhrodip.squarewise.errors.domain.ErrorCode
import java.security.Principal
import java.util.UUID
import com.subhrodip.squarewise.db.routing.DbContextHolder
import com.subhrodip.squarewise.db.routing.DbExecutionContext
import com.subhrodip.squarewise.db.routing.DbOperationKind
import com.subhrodip.squarewise.db.routing.ReadConsistency
import com.subhrodip.squarewise.observability.db.DbTelemetry
import com.subhrodip.squarewise.accounts.profile.persistence.ProfileStore

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
        profiles.get(principal.name) ?: throw ApplicationException(ErrorCode.ERR_03, "Authenticated profile not found")

    @PatchMapping(ApiEndpoints.Accounts.V1.ME)
    fun update(
        principal: Principal,
        @Valid @RequestBody request: ProfilePatchRequest
    ): ProfileResponse {
        request.validateNotEmpty()
        return profiles.update(principal.name, request)
    }

    /**
     * Translates profile-domain failures at the controller boundary so the response identifies
     * the Accounts service and retains the public RFC 7807 shape.
     *
     * @param error domain failure raised while handling a profile operation
     * @return structured problem response with the catalog HTTP status
     */
    @ExceptionHandler(ApplicationException::class)
    fun applicationFailure(error: ApplicationException): ResponseEntity<ApiProblem> {
        val status = HttpStatus.resolve(error.errorCode.httpStatus) ?: HttpStatus.INTERNAL_SERVER_ERROR
        return problem(
            code = error.errorCode,
            status = status,
            title = error.message ?: error.errorCode.safeDetail,
            detail = error.message ?: error.errorCode.safeDetail
        )
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
                    type = "https://squarewise.example/problems/${code.name.lowercase()}",
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
            ErrorCode.ERR_04 -> "FORBIDDEN"
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

    /**
     * Retrieves a profile by account ID.
     *
     * Authorized only if the caller possesses internal workload authority or is performing a
     * self-lookup matching their authenticated profile account ID. Arbitrary cross-user lookups
     * are rejected with 403 Forbidden.
     *
     * @param accountId target account identifier.
     * @param principal authenticated caller security principal.
     * @param workloadRole optional internal service trust header.
     * @return [ProfileResponse] matching the account ID.
     * @throws ApplicationException ERR_03 if unauthenticated, ERR_04 if unauthorized, or ERR_05 if not found.
     */
    @GetMapping(ApiEndpoints.Accounts.V1.PROFILES_BY_ID)
    fun getProfileById(
        @PathVariable accountId: UUID,
        principal: Principal?,
        @RequestHeader(
            value = ApiEndpoints.Headers.WORKLOAD_ROLE,
            required = false
        ) workloadRole: String? = null
    ): ProfileResponse {
        val isWorkload = workloadRole == ApiEndpoints.Headers.WORKLOAD_ROLE_INTERNAL
        if (!isWorkload) {
            val callerSubject = principal?.name ?: throw ApplicationException(ErrorCode.ERR_03, "Authentication required")
            val callerProfile = profiles.get(callerSubject) ?: throw ApplicationException(ErrorCode.ERR_03, "Authenticated profile not found")
            if (callerProfile.accountId != accountId) {
                throw ApplicationException(ErrorCode.ERR_04, "Access denied to foreign profile")
            }
        }
        return dbTelemetry.measureQuery("profile.lookup", "approved-query") {
            DbContextHolder.withContext(profileReadContext("profile.lookup")) {
                profiles.findById(accountId) ?: throw ApplicationException(ErrorCode.ERR_05, "Profile not found")
            }
        }
    }

    /**
     * Batch lookups profiles for a collection of account IDs.
     *
     * Restricted to callers with internal workload authority or end-user callers limited to
     * querying their own profile ID. Batch lookups encompassing foreign profiles by non-workload
     * callers are rejected with 403 Forbidden.
     *
     * @param request batch lookup payload with up to 100 account IDs.
     * @param principal authenticated caller security principal.
     * @param workloadRole optional internal service trust header.
     * @return list of resolved [ProfileResponse] records matching existing IDs.
     * @throws ApplicationException ERR_03 if unauthenticated or ERR_04 if unauthorized.
     */
    @PostMapping(ApiEndpoints.Accounts.V1.PROFILES_BATCH)
    fun getProfilesBatch(
        @Valid @RequestBody request: BatchProfileRequest,
        principal: Principal?,
        @RequestHeader(
            value = ApiEndpoints.Headers.WORKLOAD_ROLE,
            required = false
        ) workloadRole: String? = null
    ): List<ProfileResponse> {
        val isWorkload = workloadRole == ApiEndpoints.Headers.WORKLOAD_ROLE_INTERNAL
        if (!isWorkload) {
            val callerSubject = principal?.name ?: throw ApplicationException(ErrorCode.ERR_03, "Authentication required")
            val callerProfile = profiles.get(callerSubject) ?: throw ApplicationException(ErrorCode.ERR_03, "Authenticated profile not found")
            val requestedDistinctIds = request.accountIds.toSet()
            if (requestedDistinctIds.any { it != callerProfile.accountId }) {
                throw ApplicationException(ErrorCode.ERR_04, "Batch profile lookup requires internal workload authority")
            }
        }
        return dbTelemetry.measureQuery("profile.batch_lookup", "approved-query") {
            DbContextHolder.withContext(profileReadContext("profile.batch_lookup")) { profiles.findByIds(request.accountIds) }
        }
    }

    private fun profileReadContext(operationName: String) = DbExecutionContext(
        operationName = operationName,
        kind = DbOperationKind.QUERY,
        consistency = ReadConsistency.EVENTUAL,
        readerEligible = true
    )
}
