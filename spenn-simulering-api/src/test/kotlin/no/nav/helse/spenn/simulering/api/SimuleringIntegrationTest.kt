package no.nav.helse.spenn.simulering.api

import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.sykepenger.libs.testing.assertions.assertJsonEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SimuleringIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `arbeidsgiverrefusjon fungerer som forventet`() {
        val (faktiskResponseCode, faktiskResponseBody) =
            settOppOgUtførPostSimulering(
                request = HappyPathTestdata.ARBEIDSGIVERREFUSJON.request,
                simuleringServiceSvar = HappyPathTestdata.ARBEIDSGIVERREFUSJON.simuleringServiceSvar,
            )

        forventÉnRequestTilGandalfMedServiceUser()
        forventÉnRequestTilSimuleringService(HappyPathTestdata.ARBEIDSGIVERREFUSJON.forventetRequestTilSimuleringServiceFactory)

        assertEquals(expected = 200, actual = faktiskResponseCode)
        assertJsonEquals(
            expectedJson = HappyPathTestdata.ARBEIDSGIVERREFUSJON.forventetResponse,
            actualJson = faktiskResponseBody,
            bortsettFraStier = setOf("callId", "stacktrace"),
        )
    }

    @Test
    fun `brukerutbetaling fungerer som forventet`() {
        val (faktiskResponseCode, faktiskResponseBody) =
            settOppOgUtførPostSimulering(
                request = HappyPathTestdata.BRUKERUTBETALING.request,
                simuleringServiceSvar = HappyPathTestdata.BRUKERUTBETALING.simuleringServiceSvar,
            )

        forventÉnRequestTilGandalfMedServiceUser()
        forventÉnRequestTilSimuleringService(HappyPathTestdata.BRUKERUTBETALING.forventetRequestTilSimuleringServiceFactory)

        assertEquals(expected = 200, actual = faktiskResponseCode)
        assertJsonEquals(
            expectedJson = HappyPathTestdata.BRUKERUTBETALING.forventetResponse,
            actualJson = faktiskResponseBody,
            bortsettFraStier = setOf("callId", "stacktrace"),
        )
    }

    @Test
    fun `tom respons viderefordmidles`() {
        val (faktiskResponseCode, faktiskResponseBody) =
            settOppOgUtførPostSimulering(
                simuleringServiceSvar =
                    WireMock.okTextXml(
                        // language=xml
                        """
                        <?xml version='1.0' encoding='UTF-8'?>
                        <S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
                            <S:Body>
                                <simulerBeregningResponse xmlns="http://nav.no/system/os/tjenester/simulerFpService/simulerFpServiceGrensesnitt">
                                    <response xmlns="" />
                                </simulerBeregningResponse>
                            </S:Body>
                        </S:Envelope>
                        """.trimIndent(),
                    ),
            )

        forventÉnRequestTilGandalfMedServiceUser()
        forventÉnRequestTilSimuleringService(HappyPathTestdata.ARBEIDSGIVERREFUSJON.forventetRequestTilSimuleringServiceFactory)

        assertEquals(expected = 204, actual = faktiskResponseCode)
        assertJsonEquals(expectedJson = "", actualJson = faktiskResponseBody, bortsettFraStier = setOf("callId", "stacktrace"))
    }

    @Test
    fun `funksjonell feil fra simuleringService gir 400`() {
        val (faktiskResponseCode, faktiskResponseBody) =
            settOppOgUtførPostSimulering(
                simuleringServiceSvar =
                    WireMock
                        .aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(
                            // language=xml
                            """
                            <?xml version='1.0' encoding='UTF-8'?>
                            <S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
                                <S:Body>
                                    <S:Fault xmlns="">
                                        <faultcode>Soap:Client</faultcode>
                                        <faultstring>simulerBeregningFeilUnderBehandling</faultstring>
                                        <detail>
                                            <sf:simulerBeregningFeilUnderBehandling xmlns:sf="http://nav.no/system/os/tjenester/oppdragService">
                                                <errorMessage>UTBETALES-TIL-ID er ikke utfylt</errorMessage>
                                                <errorSource>K231BB50 section: CA10-KON</errorSource>
                                                <rootCause>Kode BB50018F - SQL      - MQ</rootCause>
                                                <dateTimeStamp>2024-01-14T09:41:29</dateTimeStamp>
                                            </sf:simulerBeregningFeilUnderBehandling>
                                        </detail>
                                    </S:Fault>
                                </S:Body>
                            </S:Envelope>
                            """.trimIndent(),
                        ),
            )

        forventÉnRequestTilGandalfMedServiceUser()
        forventÉnRequestTilSimuleringService(HappyPathTestdata.ARBEIDSGIVERREFUSJON.forventetRequestTilSimuleringServiceFactory)

        assertEquals(expected = 400, actual = faktiskResponseCode)
        assertJsonEquals(
            expectedJson =
                """
                {
                  "type": "urn:error:bad_request",
                  "title": "Bad Request",
                  "status": 400,
                  "detail": "Simulering feilet på grunn av funksjonell feil. UTBETALES-TIL-ID er ikke utfylt",
                  "instance": "/api/simulering"
                }
                """.trimIndent(),
            actualJson = faktiskResponseBody,
            bortsettFraStier = setOf("callId", "stacktrace"),
        )
    }

    @Test
    fun `teknisk feil fra simuleringService gir 500`() {
        val (faktiskResponseCode, faktiskResponseBody) =
            settOppOgUtførPostSimulering(
                simuleringServiceSvar =
                    WireMock
                        .aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(
                            """
                            <?xml version='1.0' encoding='UTF-8'?>
                            <S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
                                <S:Body>
                                    <S:Fault xmlns="">
                                        <faultcode>SOAP-ENV:Server</faultcode>
                                        <faultstring>Conversion from SOAP failed</faultstring>
                                        <detail>
                                            <CICSFault xmlns="http://www.ibm.com/software/htp/cics/WSFault">XML to data transformation failed.</CICSFault>
                                        </detail>
                                    </S:Fault>
                                </S:Body>
                            </S:Envelope>
                            """.trimIndent(),
                        ),
            )

        forventÉnRequestTilGandalfMedServiceUser()
        forventÉnRequestTilSimuleringService(HappyPathTestdata.ARBEIDSGIVERREFUSJON.forventetRequestTilSimuleringServiceFactory)

        assertEquals(expected = 500, actual = faktiskResponseCode)
        assertJsonEquals(
            expectedJson =
                """
                {
                  "type": "urn:error:internal_error",
                  "title": "Internal Server Error",
                  "status": 500,
                  "detail": "Uventet feil: XML to data transformation failed.",
                  "instance": "/api/simulering"
                }
                """.trimIndent(),
            actualJson = faktiskResponseBody,
            bortsettFraStier = setOf("callId", "stacktrace"),
        )
    }

    @Test
    fun `uforståelig svar fra simuleringService gir 500`() {
        val (faktiskResponseCode, faktiskResponseBody) =
            settOppOgUtførPostSimulering(
                simuleringServiceSvar =
                    WireMock
                        .aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("dette er ikke SOAP i det hele tatt"),
            )

        forventÉnRequestTilGandalfMedServiceUser()
        forventÉnRequestTilSimuleringService(HappyPathTestdata.ARBEIDSGIVERREFUSJON.forventetRequestTilSimuleringServiceFactory)

        assertEquals(expected = 500, actual = faktiskResponseCode)
        assertJsonEquals(
            expectedJson =
                """
                {
                  "type": "urn:error:internal_error",
                  "title": "Internal Server Error",
                  "status": 500,
                  "detail": "Uventet feil: Klarte ikke tolke SOAP-responsen",
                  "instance": "/api/simulering"
                }
                """.trimIndent(),
            actualJson = faktiskResponseBody,
            bortsettFraStier = setOf("callId", "stacktrace"),
        )
    }

    @Test
    fun `svarer 503 når Gandalf ikke gir ut SAML-token`() {
        val (responseStatus, responseBody) =
            settOppOgUtførPostSimulering(
                gandalfSvar =
                    WireMock
                        .aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{ "title": "Service Unavailable", "detail": "STS er nede" }"""),
            )

        forventÉnRequestTilGandalfMedServiceUser()
        forventIngenRequestsTilSimuleringService()

        assertEquals(503, responseStatus)
        assertJsonEquals(
            expectedJson =
                """
                {
                  "type": "urn:error:service_unavailable",
                  "title": "Service Unavailable",
                  "status": 503,
                  "detail": "Service Unavailable",
                  "instance": "/api/simulering"
                }
                """.trimIndent(),
            actualJson = responseBody,
            bortsettFraStier = setOf("callId", "stacktrace"),
        )
    }
}
