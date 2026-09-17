package com.github.navikt.tbd_libs.soap

import com.github.navikt.tbd_libs.result_object.Result
import com.github.navikt.tbd_libs.result_object.error
import com.github.navikt.tbd_libs.result_object.map
import com.github.navikt.tbd_libs.result_object.ok
import org.intellij.lang.annotations.Language
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.util.*

class MinimalSoapClient(
    private val serviceUrl: URI,
    private val tokenProvider: SamlTokenProvider,
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
    private val proxyAuthorization: (() -> Result<String>)? = null,
) {
    fun doSoapAction(
        action: String,
        body: String,
        tokenStrategy: SoapAssertionStrategy,
    ): Result<HttpResponse<String>> {
        val proxyAuthorizationResult =
            when (proxyAuthorization) {
                null -> Result.Ok(null)
                else -> proxyAuthorization()
            }
        return try {
            tokenStrategy.token(tokenProvider).map { assertion ->
                proxyAuthorizationResult.map { proxyAuthorizationToken ->
                    val requestBody = createXmlRequest(assertion, action, body)
                    val request =
                        HttpRequest
                            .newBuilder()
                            .uri(serviceUrl)
                            .header("SOAPAction", action)
                            .apply { if (proxyAuthorizationToken != null) this.header("X-Proxy-Authorization", proxyAuthorizationToken) }
                            .POST(BodyPublishers.ofString(requestBody))
                            .build()

                    httpClient.send(request, BodyHandlers.ofString())?.ok() ?: "Tom responskropp fra tjenesten".error()
                }
            }
        } catch (err: Exception) {
            err.error("Feil ved utføring av SOAP-kall: ${err.message}")
        }
    }

    private fun createXmlRequest(
        assertion: String,
        action: String,
        body: String,
        messageId: UUID = UUID.randomUUID(),
    ): String =
        defaultXmlBody
            .replace("{{action}}", action)
            .replace("{{messageId}}", "$messageId")
            .replace("{{assertion}}", assertion)
            .replace("{{body}}", body)

    private companion object {
        @Language("XML")
        private val defaultXmlBody = """<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
    <soap:Header>
        <Action xmlns="http://www.w3.org/2005/08/addressing">{{action}}</Action>
        <MessageID xmlns="http://www.w3.org/2005/08/addressing">urn:uuid:{{messageId}}</MessageID>
        <To xmlns="http://www.w3.org/2005/08/addressing">{{serviceUrl}}</To>
        <ReplyTo xmlns="http://www.w3.org/2005/08/addressing">
            <Address>http://www.w3.org/2005/08/addressing/anonymous</Address>
        </ReplyTo>
        <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
                       xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
                       soap:mustUnderstand="1">
            {{assertion}}
        </wsse:Security>
    </soap:Header>
    <soap:Body>
        {{body}}
    </soap:Body>
</soap:Envelope>"""
    }
}
