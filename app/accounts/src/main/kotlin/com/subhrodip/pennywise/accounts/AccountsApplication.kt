package com.subhrodip.pennywise.accounts

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import com.subhrodip.pennywise.errors.GlobalErrorHandler
import com.subhrodip.pennywise.errors.RequestIdFilter
import com.subhrodip.pennywise.security.OidcConfigurationGuard

@SpringBootApplication
@Import(GlobalErrorHandler::class, RequestIdFilter::class, OidcConfigurationGuard::class)
class AccountsApplication

fun main(args: Array<String>) = runApplication<AccountsApplication>(*args)
