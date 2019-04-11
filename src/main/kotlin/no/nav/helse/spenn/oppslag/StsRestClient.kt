package no.nav.helse.spenn.oppslag

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.kittinunf.fuel.httpGet
import no.nav.helse.spenn.vedtak.defaultObjectMapper
import java.time.LocalDateTime

/**
 * henter jwt token fra STS
 */
class StsRestClient(val baseUrl: String, val username: String, val password: String) {
    private var cachedOidcToken: Token? = null

    fun token(): String {
        if (Token.shouldRenew(cachedOidcToken))  {
            val (_, _, result) = "$baseUrl/rest/v1/sts/token?grant_type=client_credentials&scope=openid".httpGet()
                    .authenticate(username, password)
                    .header(mapOf("Accept" to "application/json"))
                    .response()

            cachedOidcToken = defaultObjectMapper.readValue(result.get(), Token::class.java)
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
