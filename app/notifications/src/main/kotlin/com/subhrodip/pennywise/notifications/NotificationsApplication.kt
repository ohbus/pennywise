package com.subhrodip.pennywise.notifications

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import com.subhrodip.pennywise.errors.GlobalErrorHandler
import com.subhrodip.pennywise.errors.RequestIdFilter

@SpringBootApplication
@Import(GlobalErrorHandler::class, RequestIdFilter::class)
class NotificationsApplication

fun main(args: Array<String>) = runApplication<NotificationsApplication>(*args)
