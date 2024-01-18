package no.nav.helse.spenn.simulering

import com.github.navikt.tbd_libs.soap.MinimalSoapClient
import com.github.navikt.tbd_libs.soap.SamlToken
import com.github.navikt.tbd_libs.soap.SamlTokenProvider
import com.github.navikt.tbd_libs.mock.MockHttpResponse
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.spenn.Utbetalingslinjer
import no.nav.helse.spenn.januar
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SimuleringV2ServiceTest {

    private companion object {
        private const val ENDRINGSKODE_NY = "NY"
        private const val ENDRINGSKODE_ENDRET = "ENDR"
        private const val ENDRINGSKODE_UENDRET = "UEND"
        private const val PERSON = "12345678911"
        private const val ORGNR = "123456789"
        private const val FAGSYSTEMID = "a1b0c2"
        private const val DAGSATS = 1000
        private const val GRAD = 100
        private const val SAKSBEHANDLER = "Spenn"
        private val MAKSDATO = LocalDate.MAX
        private val tidsstempel = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    @Test
    fun `håndterer feil fra OS`() {
        @Language("XML")
        val xml = """<S:Fault xmlns="">
    <faultcode>Soap:Client</faultcode>
    <faultstring>simulerBeregningFeilUnderBehandling                                             </faultstring>
    <detail>
        <sf:simulerBeregningFeilUnderBehandling xmlns:sf="http://nav.no/system/os/tjenester/oppdragService">
            <errorMessage>UTBETALES-TIL-ID er ikke utfylt</errorMessage>
            <errorSource>K231BB50 section: CA10-KON</errorSource>
            <rootCause>Kode BB50018F - SQL      - MQ</rootCause>
            <dateTimeStamp>2024-01-14T09:41:29</dateTimeStamp>
        </sf:simulerBeregningFeilUnderBehandling>
    </detail>
</S:Fault>"""

        val simulerRequest = simuleringRequest()

        val (_, simuleringClient) = mockClient(xmlResponse(xml))
        val result = simuleringClient.simulerOppdrag(simulerRequest)
        assertEquals(SimuleringStatus.FUNKSJONELL_FEIL, result.status)
    }

    @Test
    fun `håndterer cicsfeil fra OS`() {
        @Language("XML")
        val xml = """<S:Fault xmlns="">
    <faultcode>SOAP-ENV:Server</faultcode>
    <faultstring>Conversion from SOAP failed</faultstring>
    <detail>
        <CICSFault xmlns="http://www.ibm.com/software/htp/cics/WSFault">RUTINE1 17/01/2024 08:55:44 CICS01
            ERR01 1337 XML to data transformation failed. A conversion error (OUTPUT_OVERFLOW) occurred when
            converting field maksDato for WEBSERVICE simulerFpServiceWSBinding.
        </CICSFault>
    </detail>
</S:Fault>"""

        val simulerRequest = simuleringRequest()

        val (_, simuleringClient) = mockClient(xmlResponse(xml))
        val result = simuleringClient.simulerOppdrag(simulerRequest)
        assertEquals(SimuleringStatus.TEKNISK_FEIL, result.status)
    }

    private fun simuleringRequest() =
        simuleringRequestRefusjon(ENDRINGSKODE_ENDRET) {
            linje(
                Utbetalingslinjer.Utbetalingslinje(
                    1,
                    ENDRINGSKODE_NY,
                    "SPREFAG-IOP",
                    1.januar,
                    14.januar,
                    DAGSATS,
                    GRAD,
                    null,
                    null,
                    null,
                    null,
                    "DAG"
                )
            )
            linje(
                Utbetalingslinjer.Utbetalingslinje(
                    2,
                    ENDRINGSKODE_NY,
                    "SPREFAG-IOP",
                    15.januar,
                    31.januar,
                    DAGSATS,
                    GRAD,
                    null,
                    null,
                    null,
                    null,
                    "DAG"
                )
            )
        }

    private fun simuleringRequestRefusjon(
        endringskode: String,
        block: Utbetalingslinjer.() -> Unit
    ): SimulerBeregningRequest {
        val builder = SimuleringRequestBuilder(
            Utbetalingslinjer.RefusjonTilArbeidsgiver(
                PERSON,
                ORGNR,
                FAGSYSTEMID,
                endringskode,
                SAKSBEHANDLER,
                MAKSDATO
            ).apply(block)
        )
        return builder.build()
    }

    private fun xmlResponse(body: String): String {
        @Language("XML")
        val response = """<?xml version='1.0' encoding='UTF-8'?>
<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
    <S:Body>$body</S:Body>
</S:Envelope>"""
        return response
    }


    private fun mockClient(response: String): Pair<HttpClient, SimuleringV2Service> {
        val httpClient = mockk<HttpClient> {
            every {
                send<String>(any(), any())
            } returns MockHttpResponse(response)
        }
        val tokenProvider = object : SamlTokenProvider {
            override fun samlToken(username: String, password: String): SamlToken {
                throw NotImplementedError("ikke implementert i mock")
            }
        }
        val soapClient = MinimalSoapClient(URI("http://simulering-ws"), tokenProvider, httpClient)
        val client = SimuleringV2Service(
            soapClient = soapClient,
            assertionStrategy = { "<saml token>" }
        )
        return httpClient to client
    }
}