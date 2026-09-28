package com.subhrodip.pennywise.bff.graphql
import com.subhrodip.pennywise.bff.transport.model.output.BffProfile

import com.subhrodip.pennywise.bff.transport.AccountsGateway
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import reactor.core.publisher.Mono

@Controller
class ProfileGraphqlController(private val gateway: AccountsGateway) {
    @QueryMapping
    fun me(@AuthenticationPrincipal(expression = "tokenValue") principal: Any?): Mono<BffProfile> = gateway.getMe(bearerToken(principal))
}
