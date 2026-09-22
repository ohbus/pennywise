package com.subhrodip.pennywise.accounts.profile.api

import com.subhrodip.pennywise.errors.domain.ApplicationException
import com.subhrodip.pennywise.errors.domain.ErrorCode
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/** Partial profile update input; at least one field must be supplied. */
data class ProfilePatchRequest(
    @field:NotBlank @field:Size(max = 120) val displayName: String? = null,
    @field:NotBlank @field:Size(max = 80) val timezone: String? = null,
    @field:Pattern(regexp = "^[A-Z]{3}$") val defaultCurrency: String? = null
) {
    /** Rejects an empty PATCH request before persistence side effects. */
    fun validateNotEmpty() {
        if (displayName == null && timezone == null && defaultCurrency == null) {
            throw ApplicationException(ErrorCode.ERR_02, "At least one profile field is required")
        }
    }
}
