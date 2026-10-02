package com.subhrodip.squarewise.expensecore

import org.junit.jupiter.api.Test

/** Verifies the Expense Core command-line entrypoint can bootstrap a non-web test application. */
class ApplicationEntrypointTest {
    @Test
    fun `main bootstraps the non-web test profile`() {
        main(
            arrayOf(
                "--spring.profiles.active=test",
                "--spring.main.web-application-type=none",
                "--spring.main.register-shutdown-hook=false",
                "--spring.datasource.url=jdbc:h2:mem:expense-entrypoint;DB_CLOSE_DELAY=-1"
            )
        )
    }
}
