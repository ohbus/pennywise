package com.subhrodip.pennywise.bff.graphql

import com.subhrodip.pennywise.bff.*
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Mono
import java.security.Principal

@Controller
class ProfileGraphqlController(private val gateway: AccountsGateway) {
    @QueryMapping
    fun me(principal: Principal?): Mono<BffProfile> =
        gateway.getMe(principal?.name)
}
