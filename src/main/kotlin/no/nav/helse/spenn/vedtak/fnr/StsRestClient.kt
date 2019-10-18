package no.nav.helse.spenn.vedtak.fnr

import org.json.JSONObject
import java.time.LocalDateTime
import java.util.*

//@Component
/*class StsRestClient(/*@Value("\${SECURITY_TOKEN_SERVICE_REST_URL}")*/ val stsRestUrl: String,
                    /*@Value("\${STS_REST_USERNAME}")*/ val stsRestUsername: String,
                    /*@Value("\${STS_REST_PASSWORD}")*/ val stsRestPassword: String) {*/
class StsRestClient(val baseUrl: String, val username: String, val password: String) {
    private var cachedOidcToken: Token? = null

    fun token(): String {
        if (Token.shouldRenew(cachedOidcToken)) {
            val encodedAuth = String(Base64.getEncoder().encode("$username:$password".toByteArray()))
            cachedOidcToken =
                    khttp.get(
                            url = "$baseUrl/rest/v1/sts/token?grant_type=client_credentials&scope=openid",
                            headers = mapOf(
                                    "Authorization" to "Basic $encodedAuth",
                                    "Accept" to "application/json"
                            )).jsonObject.mapToToken()
        }
        return cachedOidcToken!!.accessToken
    }

    private fun JSONObject.mapToToken(): Token {
        return Token(getString("access_token"),
                getString("token_type"),
                getInt("expires_in"))
    }

    data class Token(val accessToken: String, val type: String, val expiresIn: Int) {
        // expire 10 seconds before actual expiry. for great margins.
        val expirationTime: LocalDateTime = LocalDateTime.now().plusSeconds(expiresIn - 10L)

        companion object {
            fun shouldRenew(token: Token?) =
                    token == null || isExpired(token)

            private fun isExpired(token: Token): Boolean {
                return token.expirationTime.isBefore(LocalDateTime.now())
            }
        }
    }
}
