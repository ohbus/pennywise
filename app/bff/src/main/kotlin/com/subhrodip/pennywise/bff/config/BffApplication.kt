package com.subhrodip.pennywise.bff

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import com.subhrodip.pennywise.security.OidcConfigurationGuard

@SpringBootApplication
@Import(OidcConfigurationGuard::class)
class BffApplication {
    @Bean
    fun liveUpdateFanout(): LiveUpdateFanout = LiveUpdateFanout()
}

fun main(args: Array<String>) = runApplication<BffApplication>(*args)
