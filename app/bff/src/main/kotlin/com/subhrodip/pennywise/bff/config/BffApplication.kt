package com.subhrodip.pennywise.bff

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class BffApplication {
    @Bean
    fun liveUpdateFanout(): LiveUpdateFanout = LiveUpdateFanout()
}

fun main(args: Array<String>) = runApplication<BffApplication>(*args)
