package com.github.navikt.tbd_libs.soap

import com.github.navikt.tbd_libs.mock.MockHttpResponse
import com.github.navikt.tbd_libs.mock.bodyAsString
import com.github.navikt.tbd_libs.result_object.Result
import com.github.navikt.tbd_libs.result_object.ok
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

class MinimalSoapClientTest {
    private companion object {
        private const val USERNAME = "username"
        private const val PASSWORD = "password"

        private val SERVICE_URL = URI("http://localhost:8080/Ønskeliste_V1")

        @Language("XML")
        private val SAML_TOKEN_ASSERTION = """<saml2:Assertion xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion"/>"""

        @Language("XML")
        private val UNT_ASSERTION = """<wsse:UsernameToken>
    <wsse:Username>$USERNAME</wsse:Username>
    <wsse:Password>$PASSWORD</wsse:Password>
</wsse:UsernameToken>"""
    }

    @Test
    fun `lager soap request med saml token`() {
        val action = "http://nordpolen.no/tjeneste/virksomhet/julenissen/v1/Ønskeliste_V1/finnJulegaveListeRequest"

        @Language("XML")
        val body = "<finnJulegaveListe><request><ident>12345678911</ident></request></finnJulegaveListe>"
        val expectedResponseBody = "Hello"
        val (httpClient, soapClient) = mockClient(expectedResponseBody)
        val result = soapClient.doSoapAction(action, body, samlStrategy(USERNAME, PASSWORD))
        result as com.github.navikt.tbd_libs.result_object.Result.Ok
        assertEquals(expectedResponseBody, result.value.body())
        verifiserSoapRequest(httpClient, action, body)
    }

    @Test
    fun `lager soap request med brukernavn og passord`() {
        val action = "http://nordpolen.no/tjeneste/virksomhet/julenissen/v1/Ønskeliste_V1/finnJulegaveListeRequest"

        @Language("XML")
        val body = "<finnJulegaveListe><request><ident>12345678911</ident></request></finnJulegaveListe>"
        val expectedResponseBody = "Hello"
        val (httpClient, soapClient) = mockClient(expectedResponseBody)
        val result = soapClient.doSoapAction(action, body, usernamePasswordStrategy(USERNAME, PASSWORD))
        result as Result.Ok
        assertEquals(expectedResponseBody, result.value.body())
        verifiserSoapRequest(httpClient, action, body, UNT_ASSERTION)
    }

    private fun verifiserSoapRequest(
        httpClient: HttpClient,
        action: String,
        body: String,
        samlAssertion: String = SAML_TOKEN_ASSERTION,
    ) {
        val inneholderSOAPAction = { it: HttpRequest ->
            it.headers().firstValue("SOAPAction").getOrNull() == action
        }
        val inneholderSAMLAssertion = { it: HttpRequest ->
            it.bodyAsString().contains(samlAssertion)
        }
        val inneholderSOAPRequestBody = { it: HttpRequest ->
            it.bodyAsString().contains(body)
        }
        verify(exactly = 1) {
            httpClient.send<String>(
                match {
                    inneholderSOAPAction(it) && inneholderSAMLAssertion(it) && inneholderSOAPRequestBody(it)
                },
                any(),
            )
        }
    }

    private fun mockClient(
        response: String,
        statusCode: Int = 200,
    ): Pair<HttpClient, MinimalSoapClient> {
        val httpClient =
            mockk<HttpClient> {
                every {
                    send<String>(any(), any())
                } returns MockHttpResponse(response, statusCode)
            }
        val tokenProvider =
            object : SamlTokenProvider {
                override fun samlToken(
                    username: String,
                    password: String,
                ): Result<SamlToken> = SamlToken(SAML_TOKEN_ASSERTION, LocalDateTime.MAX).ok()
            }
        val soapClient = MinimalSoapClient(SERVICE_URL, tokenProvider, httpClient)
        return httpClient to soapClient
    }
}
