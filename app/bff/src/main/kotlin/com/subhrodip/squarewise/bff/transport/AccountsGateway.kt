@file:Suppress("CanConvertToMultiDollarString")

package com.subhrodip.squarewise.bff.transport
import com.subhrodip.squarewise.bff.transport.model.output.BffProfile
import com.subhrodip.squarewise.bff.transport.UpstreamServiceException

import com.subhrodip.squarewise.bff.transport.BffGatewayFilters
import com.subhrodip.squarewise.ids.contracts.ApiEndpoints
import java.time.Duration
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/** REST gateway for authenticated Accounts profile calls. */
@Component
class AccountsGateway(
    builder: WebClient.Builder,
    @Value("${'$'}{squarewise.accounts-url}") baseUrl: String,
    @Value("${'$'}{squarewise.bff.upstream-timeout:2s}") private val timeout: Duration
) {
    private val client = builder.filter(BffGatewayFilters.bearerPropagation).baseUrl(baseUrl).build()

    fun getMe(bearer: String?): Mono<BffProfile> =
        client.get().uri(ApiEndpoints.Accounts.V1.PATH_ME)
            .headers { headers -> bearer?.let { headers.setBearerAuth(it) } }
            .retrieve()
            .onStatus({ it.isError }) { response -> Mono.error(UpstreamServiceException(response.statusCode().value(), "Accounts returned HTTP ${response.statusCode().value()}")) }
            .bodyToMono(BffProfile::class.java)
            .timeout(timeout)
}
