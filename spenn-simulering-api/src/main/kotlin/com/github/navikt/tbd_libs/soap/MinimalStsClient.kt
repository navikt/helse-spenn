package com.github.navikt.tbd_libs.soap

import com.github.navikt.tbd_libs.result_object.Result
import com.github.navikt.tbd_libs.result_object.error
import com.github.navikt.tbd_libs.result_object.map
import com.github.navikt.tbd_libs.result_object.ok
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.introspect.DefaultAccessorNamingStrategy
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.*

class MinimalStsClient(
    private val baseUrl: URI,
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
    private val objectMapper: ObjectMapper =
        jacksonMapperBuilder()
            .accessorNaming(DefaultAccessorNamingStrategy.Provider().withFirstCharAcceptance(true, true))
            .build(),
    private val proxyAuthorization: (() -> Result<String>)? = null,
) : SamlTokenProvider {
    override fun samlToken(
        username: String,
        password: String,
    ): Result<SamlToken> =
        try {
            requestSamlToken(username, password).map { body ->
                decodeSamlTokenResponse(body)
            }
        } catch (err: Exception) {
            err.error("Feil ved henting av SAML-token: ${err.message}")
        }

    private fun requestSamlToken(
        username: String,
        password: String,
    ): Result<String> {
        val encodedCredentials =
            Base64
                .getEncoder()
                .encodeToString("$username:$password".toByteArray(StandardCharsets.UTF_8))
        val proxyAuthorizationToken =
            when (proxyAuthorization) {
                null -> null
                else ->
                    when (val result = proxyAuthorization()) {
                        is Result.Error -> return result
                        is Result.Ok -> result.value
                    }
            }
        val request =
            HttpRequest
                .newBuilder(URI("$baseUrl/rest/v1/sts/samltoken"))
                .header("Authorization", "Basic $encodedCredentials")
                .apply { if (proxyAuthorizationToken != null) this.header("X-Proxy-Authorization", proxyAuthorizationToken) }
                .GET()
                .build()

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body()?.ok() ?: "Tom responskropp fra STS".error()
    }

    private fun decodeSamlTokenResponse(body: String): Result<SamlToken> {
        val node =
            try {
                objectMapper.readTree(body)
            } catch (err: Exception) {
                return err.error("Kunne ikke tolke JSON fra responsen til STS: $body")
            }
        return extractSamlTokenFromResponse(node) ?: handleErrorResponse(node)
    }

    private fun extractSamlTokenFromResponse(node: JsonNode): Result<SamlToken>? {
        val accessToken = node.path("access_token").takeIf(JsonNode::isString)?.asString() ?: return null
        val issuedTokenType = node.path("issued_token_type").takeIf(JsonNode::isString)?.asString() ?: return null
        val expiresIn = node.path("expires_in").takeIf(JsonNode::isNumber)?.asLong() ?: return null
        if (issuedTokenType != "urn:ietf:params:oauth:token-type:saml2") return "Ukjent token type: $issuedTokenType".error()
        return try {
            SamlToken(
                Base64.getDecoder().decode(accessToken).decodeToString(),
                LocalDateTime.now().plusSeconds(expiresIn),
            ).ok()
        } catch (err: Exception) {
            err.error("Kunne ikke dekode Base64: ${err.message}")
        }
    }

    private fun handleErrorResponse(node: JsonNode): Result<SamlToken> {
        val title = node.path("title").asString()
        val errorDetail = node.path("detail").takeIf(JsonNode::isString) ?: return "Ukjent respons fra STS: $node".error()
        return "Feil fra STS: $title - $errorDetail".error()
    }
}
