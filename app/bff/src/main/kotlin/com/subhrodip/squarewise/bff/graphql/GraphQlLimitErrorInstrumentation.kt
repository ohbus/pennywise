package com.subhrodip.squarewise.bff.graphql

import graphql.ExecutionResult
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.execution.instrumentation.SimplePerformantInstrumentation
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters
import java.util.concurrent.CompletableFuture
import java.util.Locale
import java.util.UUID

/** Adds the application code to pre-execution depth and complexity failures. */
class GraphQlLimitErrorInstrumentation : SimplePerformantInstrumentation() {
    override fun instrumentExecutionResult(
        executionResult: ExecutionResult,
        parameters: InstrumentationExecutionParameters,
        instrumentationState: InstrumentationState?
    ): CompletableFuture<ExecutionResult> {
        val errors = executionResult.errors.map { error ->
            if (error.extensions?.containsKey("code") == true) {
                error
            } else {
                val code = if (isQueryLimitError(error)) "RATE_LIMITED" else "VALIDATION_FAILED"
                GraphqlErrorBuilder.newError()
                    .message(if (code == "RATE_LIMITED") "GraphQL query limit exceeded" else "GraphQL request is invalid")
                    .errorType(error.errorType)
                    .locations(error.locations)
                    .extensions(
                        buildMap<String, Any> {
                            put("code", code)
                            put("requestId", UUID.randomUUID().toString())
                            if (code == "RATE_LIMITED") put("retryAfterSeconds", 60)
                        }
                    )
                    .build()
            }
        }
        return CompletableFuture.completedFuture(executionResult.transform { it.errors(errors) })
    }

    private fun isQueryLimitError(error: GraphQLError): Boolean {
        val message = error.message.lowercase(Locale.ROOT)
        return "maximum query complexity" in message || "maximum query depth" in message
    }
}
