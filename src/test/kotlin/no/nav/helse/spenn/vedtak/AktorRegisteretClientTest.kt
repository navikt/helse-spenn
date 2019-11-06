package no.nav.helse.spenn.vedtak

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spenn.kWhen
import no.nav.helse.spenn.vedtak.fnr.AktorNotFoundException
import no.nav.helse.spenn.vedtak.fnr.AktorRegisteretClient
import no.nav.helse.spenn.vedtak.fnr.StsRestClient
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.mock

class AktorRegisteretClientTest {

    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())

        @BeforeAll
        @JvmStatic
        fun start() {
            server.start()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            server.stop()
        }
    }

    val mockStsRest = mock(StsRestClient::class.java)

    private val aktørregisterClient = AktorRegisteretClient(mockStsRest, server.baseUrl())

    @BeforeEach
    fun configure() {
        WireMock.configureFor(server.port())
        kWhen(mockStsRest.token()).thenReturn("foobar")
    }

    @Test
    fun `should return gjeldende identer by aktoerId`() {
        WireMock.stubFor(aktørregisterRequestMapping
                .withHeader("Nav-Personidenter", WireMock.equalTo("1573082186699"))
                .willReturn(WireMock.ok(ok_aktoerId_response)))

        val lookupResult = aktørregisterClient.tilFnr("1573082186699")

        assertEquals("12345678911", lookupResult)
    }

    @Test
    fun `should return feil when ident does not exist`() {
        WireMock.stubFor(aktørregisterRequestMapping
                .withHeader("Nav-Personidenter", WireMock.equalTo("1573082186698"))
                .willReturn(WireMock.ok(id_not_found_response)))

        assertThrows(AktorNotFoundException::class.java) {
            aktørregisterClient.tilFnr("1573082186698")
        }
    }
}

private val aktørregisterRequestMapping = WireMock.get(WireMock.urlPathEqualTo("/api/v1/identer"))
        .withQueryParam("gjeldende", WireMock.equalTo("true"))
        .withHeader("Authorization", WireMock.equalTo("Bearer foobar"))
        .withHeader("Nav-Call-Id", WireMock.containing("-"))
        .withHeader("Nav-Consumer-Id", WireMock.equalTo("spenn"))
        .withHeader("Accept", WireMock.equalTo("application/json"))

private val ok_aktoerId_response = """
{
  "1573082186699": {
    "identer": [
      {
        "ident": "1573082186699",
        "identgruppe": "AktoerId",
        "gjeldende": true
      },
      {
        "ident": "12345678911",
        "identgruppe": "NorskIdent",
        "gjeldende": true
      }
    ],
    "feilmelding": null
  }
}""".trimIndent()

private val id_not_found_response = """
{
    "1573082186698": {
        "identer": null,
        "feilmelding": "Den angitte personidenten finnes ikke"
    }
}
""".trimIndent()
