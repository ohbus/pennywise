package com.subhrodip.pennywise.expensecore

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import com.subhrodip.pennywise.errors.GlobalErrorHandler
import com.subhrodip.pennywise.errors.RequestIdFilter
import com.subhrodip.pennywise.security.OidcConfigurationGuard

@SpringBootApplication
@Import(GlobalErrorHandler::class, RequestIdFilter::class, OidcConfigurationGuard::class)
class ExpenseCoreApplication


fun main(args: Array<String>) = runApplication<ExpenseCoreApplication>(*args)
