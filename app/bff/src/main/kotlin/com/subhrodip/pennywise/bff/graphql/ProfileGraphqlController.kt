package com.subhrodip.pennywise.bff.graphql

import com.subhrodip.pennywise.bff.*
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.core.annotation.AuthenticationPrincipal
import java.security.Principal
import org.springframework.stereotype.Controller
import reactor.core.publisher.Mono

@Controller
class ProfileGraphqlController(private val gateway: AccountsGateway) {
    @QueryMapping
    fun me(@AuthenticationPrincipal(expression = "tokenValue") principal: Any?): Mono<BffProfile> = gateway.getMe(bearerToken(principal))
}
