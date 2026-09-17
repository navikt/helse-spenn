package no.nav.helse.spenn.simulering.api

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.verification.LoggedRequest
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.ContentType.APPLICATION_JSON
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.util.Timeout
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.net.ServerSocket
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.Base64.getEncoder
import kotlin.test.assertEquals

abstract class AbstractIntegrationTest {
    protected val mockOAuth2Server = MockOAuth2Server().also(MockOAuth2Server::start)

    protected val gandalfWireMock =
        WireMockServer(wireMockConfig().dynamicPort())
            .also(WireMockServer::start)

    protected val simuleringServiceWireMock =
        WireMockServer(wireMockConfig().dynamicPort())
            .also(WireMockServer::start)

    private val httpPort = ServerSocket(0).use(ServerSocket::getLocalPort)

    private val app =
        lagApplikasjon(
            env =
                mapOf(
                    "HTTP_PORT" to httpPort.toString(),
                    "AZURE_APP_CLIENT_ID" to CLIENT_ID,
                    "AZURE_OPENID_CONFIG_ISSUER" to mockOAuth2Server.issuerUrl("default").toString(),
                    "AZURE_OPENID_CONFIG_JWKS_URI" to mockOAuth2Server.jwksUrl("default").toString(),
                    "GANDALF_BASE_URL" to gandalfWireMock.baseUrl(),
                    "SIMULERING_SERVICE_URL" to "${simuleringServiceWireMock.baseUrl()}/simulerFpServiceWSBinding",
                ),
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT, PrometheusRegistry(), Clock.SYSTEM),
        )

    val serviceUserUsername = "srvspenn-${UUID.randomUUID()}"
    val serviceUserPassword = "et-passord"
    val samlToken = """<saml2:Assertion xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion" ID="${UUID.randomUUID()}"/>"""

    @BeforeEach
    fun setUp() {
        app.start(wait = false)
        ventTilApplikasjonenErKlar()
        gandalfWireMock.resetAll()
        simuleringServiceWireMock.resetAll()
    }

    @AfterEach
    fun teardown() {
        app.stop(gracePeriodMillis = 0, timeoutMillis = 10_000)
        gandalfWireMock.stop()
        simuleringServiceWireMock.stop()
        mockOAuth2Server.shutdown()
    }

    private fun ventTilApplikasjonenErKlar() {
        val frist = Instant.now().plus(Duration.ofSeconds(30))
        while (Instant.now() < frist) {
            if (isreadyGir200()) return
            Thread.sleep(50)
        }
        error("Applikasjonen brukte for lang tid på å starte opp")
    }

    private fun isreadyGir200(): Boolean =
        runCatching {
            Request
                .get("http://localhost:$httpPort/isready")
                .connectTimeout(Timeout.ofSeconds(1))
                .responseTimeout(Timeout.ofSeconds(1))
                .execute()
                .handleResponse { respons -> respons.code == 200 }
        }.getOrDefault(false)

    protected fun settOppOgUtførPostSimulering(
        @Language("JSON")
        request: String = HappyPathTestdata.ARBEIDSGIVERREFUSJON.request,
        bearerToken: String? = bearerToken(),
        serviceUserUsername: String? = this.serviceUserUsername,
        serviceUserPassword: String? = this.serviceUserPassword,
        gandalfSvar: ResponseDefinitionBuilder =
            WireMock.okJson(
                """
                {
                  "access_token": "${getEncoder().encodeToString(samlToken.toByteArray())}",
                  "issued_token_type": "urn:ietf:params:oauth:token-type:saml2",
                  "expires_in": 3600
                }
                """.trimIndent(),
            ),
        simuleringServiceSvar: ResponseDefinitionBuilder = HappyPathTestdata.ARBEIDSGIVERREFUSJON.simuleringServiceSvar,
    ): Pair<Int, String> {
        gandalfWireMock.stubFor(
            WireMock
                .get(WireMock.urlPathEqualTo("/rest/v1/sts/samltoken"))
                .willReturn(gandalfSvar),
        )

        simuleringServiceWireMock.stubFor(
            WireMock
                .post(WireMock.urlPathEqualTo("/simulerFpServiceWSBinding"))
                .willReturn(simuleringServiceSvar),
        )

        return HttpClients.custom().disableAutomaticRetries().build().use { httpClient ->
            Request
                .post("http://localhost:$httpPort/api/simulering")
                .bodyString(request, APPLICATION_JSON)
                .apply {
                    if (bearerToken != null) addHeader("Authorization", "Bearer $bearerToken")
                    if (serviceUserUsername != null) addHeader("X-ServiceUser-Username", serviceUserUsername)
                    if (serviceUserPassword != null) addHeader("X-ServiceUser-Password", serviceUserPassword)
                }.execute(httpClient)
                .handleResponse { it.code to it.entity?.let(EntityUtils::toString).orEmpty() }
        }
    }

    private fun requestsTilGandalf(): List<LoggedRequest> = gandalfWireMock.findAll(WireMock.getRequestedFor(WireMock.urlPathEqualTo("/rest/v1/sts/samltoken")))

    protected fun forventIngenRequestsTilGandalf() {
        assertEquals(0, requestsTilGandalf().size)
    }

    protected fun forventÉnRequestTilGandalfMedServiceUser() {
        val requestTilGandalf = requestsTilGandalf().assertedSingle()
        assertEquals(
            "Basic ${Base64.getEncoder().encodeToString("$serviceUserUsername:$serviceUserPassword".toByteArray())}",
            requestTilGandalf.getHeader("Authorization"),
        )
        assertEquals("", requestTilGandalf.bodyAsString)
    }

    private fun requestsTilSimuleringService(): List<LoggedRequest> = simuleringServiceWireMock.findAll(WireMock.postRequestedFor(WireMock.urlPathEqualTo("/simulerFpServiceWSBinding")))

    protected fun forventIngenRequestsTilSimuleringService() {
        assertEquals(0, requestsTilSimuleringService().size)
    }

    protected fun forventÉnRequestTilSimuleringService(forventetRequestFunction: (samlToken: String) -> String) {
        val requestTilSimuleringService = requestsTilSimuleringService().assertedSingle()
        assertEquals(
            "http://nav.no/system/os/tjenester/simulerFpService/simulerFpServiceGrensesnitt/simulerFpService/simulerBeregningRequest",
            requestTilSimuleringService.getHeader("SOAPAction"),
        )
        assertEquals(
            forventetRequestFunction(samlToken).removeMessageId().removeIndents(),
            requestTilSimuleringService.bodyAsString.removeMessageId().removeIndents(),
        )
    }

    protected fun String.removeMessageId(): String = replace(Regex("urn:uuid:[0-9a-fA-F-]{36}"), "urn:uuid:{{messageId}}")

    protected fun String.removeIndents(): String =
        lines()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .joinToString(separator = "\n")

    protected fun bearerToken(
        audience: String = CLIENT_ID,
        claims: Map<String, String> = mapOf("azp_name" to "spenn-simulering"),
    ): String = mockOAuth2Server.issueToken(audience = audience, claims = claims).serialize()

    private fun <T> Collection<T>.assertedSingle(): T {
        assertEquals(1, size)
        return single()
    }

    companion object {
        const val CLIENT_ID = "spenn-simulering-api-e2e"
    }
}
