package com.subhrodip.squarewise.bff

import org.junit.jupiter.api.Test

/** Verifies the BFF command-line entrypoint can bootstrap a non-web test application. */
class ApplicationEntrypointTest {
    @Test
    fun `main bootstraps the non-web test profile`() {
        main(
            arrayOf(
                "--spring.profiles.active=test",
                "--spring.main.web-application-type=none",
                "--spring.main.register-shutdown-hook=false",
                "--squarewise.accounts-url=http://localhost:18081",
                "--squarewise.expense-core-url=http://localhost:18082"
            )
        )
    }
}
