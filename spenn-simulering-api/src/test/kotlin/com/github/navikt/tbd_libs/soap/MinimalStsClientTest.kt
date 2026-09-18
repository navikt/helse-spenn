package com.github.navikt.tbd_libs.soap

import com.github.navikt.tbd_libs.mock.MockHttpResponse
import com.github.navikt.tbd_libs.result_object.Result
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlin.jvm.optionals.getOrNull

class MinimalStsClientTest {
    private companion object {
        private const val USERNAME = "username"
        private const val PASSWORD = "password"
        private const val TOKEN = "<saml token>"

        private fun String.encodeBase64() = Base64.getEncoder().encodeToString(toByteArray(StandardCharsets.UTF_8))
    }

    @Test
    fun `dekoder token fra base64`() {
        val base64Token = TOKEN.encodeBase64()
        val (httpClient, stsClient) = mockClient(tokenResponse(base64Token))

        val result = stsClient.samlToken(USERNAME, PASSWORD)

        result as Result.Ok
        assertEquals(TOKEN, result.value.token)
        verifiserRequest(httpClient) {
            it.uri() == URI("http://localhost/rest/v1/sts/samltoken") &&
                it.headers().firstValue("Authorization").getOrNull() == "Basic ${"$USERNAME:$PASSWORD".encodeBase64()}"
        }
    }

    @Test
    fun `håndterer at token ikke er saml`() {
        val base64Token = TOKEN.encodeBase64()
        val (_, stsClient) = mockClient(tokenResponse(base64Token, "jwt"))
        val result = stsClient.samlToken(USERNAME, PASSWORD)
        result as Result.Error
        assertEquals("Ukjent token type: jwt", result.error)
    }

    @Test
    fun `håndterer at token ikke er base 64`() {
        val (_, stsClient) = mockClient(tokenResponse(TOKEN))
        val result = stsClient.samlToken(USERNAME, PASSWORD)
        result as Result.Error
        assertEquals("Kunne ikke dekode Base64: Illegal base64 character 3c", result.error)
    }

    @Test
    fun `håndterer feil fra server`() {
        val (_, stsClient) = mockClient(errorResponse())
        val result = stsClient.samlToken(USERNAME, PASSWORD)
        result as Result.Error
        assertEquals("Feil fra STS: Method Not Allowed - \"Method 'POST' is not supported.\"", result.error)
    }

    @Test
    fun `håndterer ugyldig json i response`() {
        val (_, stsClient) = mockClient("Internal Server Error")
        val result = stsClient.samlToken(USERNAME, PASSWORD)
        result as Result.Error
        assertEquals("Kunne ikke tolke JSON fra responsen til STS: Internal Server Error", result.error)
    }

    private fun verifiserRequest(
        httpClient: HttpClient,
        sjekk: (HttpRequest) -> Boolean,
    ) {
        verify {
            httpClient.send<String>(
                match {
                    sjekk(it)
                },
                any(),
            )
        }
    }

    private fun mockClient(response: String): Pair<HttpClient, MinimalStsClient> {
        val httpClient =
            mockk<HttpClient> {
                every {
                    send<String>(any(), any())
                } returns MockHttpResponse(response)
            }
        val stsClient = MinimalStsClient(URI("http://localhost"), httpClient)
        return httpClient to stsClient
    }

    private fun tokenResponse(
        token: String,
        tokenType: String = "urn:ietf:params:oauth:token-type:saml2",
    ): String {
        @Language("JSON")
        val response = """{
            "access_token": "$token",
            "issued_token_type": "$tokenType",
            "token_type": "Bearer",
            "expires_in": 3600
        }"""
        return response
    }

    private fun errorResponse(): String {
        @Language("JSON")
        val response = """{
            "type": "about:blank",
            "title": "Method Not Allowed",
            "status": 405,
            "detail": "Method 'POST' is not supported.",
            "instance": "/rest/v1/sts/samltoken"
        }"""
        return response
    }
}
