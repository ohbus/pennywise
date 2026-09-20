package com.subhrodip.pennywise.bff.graphql

import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.IntValue
import graphql.language.StringValue
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.GraphQLScalarType
import graphql.schema.idl.RuntimeWiring
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.Locale

class GraphQlScalarConfigurationTest {

    @Test
    fun `MoneyMinor accepts signed integer values and literals`() {
        val scalar = scalar("MoneyMinor")
        val coercing = scalar.coercing

        assertEquals("-12", coercing.parseValue("-12", GraphQLContext.getDefault(), Locale.ROOT))
        assertEquals("42", coercing.parseLiteral(IntValue.of(42), CoercedVariables.emptyVariables(), GraphQLContext.getDefault(), Locale.ROOT))
        assertEquals("900", coercing.parseLiteral(StringValue.of("900"), CoercedVariables.emptyVariables(), GraphQLContext.getDefault(), Locale.ROOT))
    }

    @Test
    fun `MoneyMinor rejects malformed values`() {
        val coercing = scalar("MoneyMinor").coercing

        assertThrows(CoercingParseValueException::class.java) {
            coercing.parseValue("12.5", GraphQLContext.getDefault(), Locale.ROOT)
        }
        assertThrows(CoercingParseLiteralException::class.java) {
            coercing.parseLiteral(StringValue.of(" "), CoercedVariables.emptyVariables(), GraphQLContext.getDefault(), Locale.ROOT)
        }
    }

    @Test
    fun `DateTime validates ISO instant and serializes values`() {
        val coercing = scalar("DateTime").coercing
        val context = GraphQLContext.getDefault()

        assertEquals("2026-09-18T10:00:00Z", coercing.parseValue("2026-09-18T10:00:00Z", context, Locale.ROOT))
        assertEquals("2026-09-18T10:00:00Z", coercing.serialize("2026-09-18T10:00:00Z", context, Locale.ROOT))
        assertThrows(CoercingParseValueException::class.java) {
            coercing.parseValue("not-a-date", context, Locale.ROOT)
        }
    }

    private fun scalar(name: String): GraphQLScalarType {
        val builder = RuntimeWiring.newRuntimeWiring()
        GraphQlScalarConfiguration().runtimeWiringConfigurer().configure(builder)
        return builder.build().scalars[name] ?: error("Scalar $name was not registered")
    }
}
