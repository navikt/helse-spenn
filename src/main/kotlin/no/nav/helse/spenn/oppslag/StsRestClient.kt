package no.nav.helse.spenn.oppslag

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.helse.Environment
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDateTime


@Component
class StsRestClient(val env: Environment) {
    private var cachedOidcToken: Token? = null

    fun token(): String {
        if (Token.shouldRenew(cachedOidcToken))  {
           val webClient = WebClient.builder().baseUrl(env.stsRestUrl)
                   .filter(ExchangeFilterFunctions.basicAuthentication(env.stsRestUsername, env.stsRestPassword))
                   .build()
            cachedOidcToken = webClient.get().uri("/rest/v1/sts/token?grant_type=client_credentials&scope=openid")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Token::class.java)
                    .block()
        }
        return cachedOidcToken!!.accessToken
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Token(@JsonProperty("access_token") val accessToken: String, @JsonProperty("token_type") val type: String, @JsonProperty("expires_in") val expiresIn: Int) {
        // expire 10 seconds before actual expiry. for great margins.
        val expirationTime: LocalDateTime = LocalDateTime.now().plusSeconds(expiresIn - 10L)

        companion object {
            fun shouldRenew(token: Token?): Boolean {
                if (token == null) {
                    return true
                }

                return isExpired(token)
            }

            fun isExpired(token: Token): Boolean {
                return token.expirationTime.isBefore(LocalDateTime.now())
            }
        }
    }
}
