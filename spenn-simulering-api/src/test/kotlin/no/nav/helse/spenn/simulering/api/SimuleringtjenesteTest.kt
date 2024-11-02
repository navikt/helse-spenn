package no.nav.helse.spenn.simulering.api

import com.github.navikt.tbd_libs.mock.MockHttpResponse
import com.github.navikt.tbd_libs.soap.MinimalSoapClient
import com.github.navikt.tbd_libs.soap.SamlToken
import com.github.navikt.tbd_libs.soap.SamlTokenProvider
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.spenn.simulering.api.client.SimuleringV2Service
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import java.net.URI
import java.net.http.HttpClient
import java.time.LocalDate

class SimuleringtjenesteTest {

    private companion object {
        private const val PERSON = "12345678911"
        private const val ORGNR = "123456789"
        private const val FAGSYSTEMID = "a1b0c2"
        private const val DAGSATS = 1000
        private const val GRAD = 100
        private const val SAKSBEHANDLER = "Spenn"
        private val MAKSDATO = LocalDate.MAX
    }

    @Test
    fun `håndterer ok simulering med ingen resultat`() {
        @Language("XML")
        val xml = """<simulerBeregningResponse xmlns="http://nav.no/system/os/tjenester/simulerFpService/simulerFpServiceGrensesnitt">
    <response xmlns="">
        <simulering>
            <gjelderId>12345678911</gjelderId>
            <gjelderNavn>NORMAL MUFFINS</gjelderNavn>
            <datoBeregnet>2018-01-17</datoBeregnet>
            <kodeFaggruppe>KORTTID</kodeFaggruppe>
            <belop>0.00</belop>
        </simulering>
    </response>
</simulerBeregningResponse>"""

        val simulerRequest = simuleringRequest()

        val (_, simuleringClient) = mockClient(xmlResponse(xml))
        val result = simuleringClient.simulerOppdrag(simulerRequest)
        assertInstanceOf<SimuleringResponse.Ok>(result)
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
        assertInstanceOf<SimuleringResponse.FunksjonellFeil>(result)
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
        assertInstanceOf<SimuleringResponse.TekniskFeil>(result)
    }

    private fun simuleringRequest() =
        SimuleringRequest(
            fødselsnummer = PERSON,
            oppdrag = SimuleringRequest.Oppdrag(
                fagområde = SimuleringRequest.Oppdrag.Fagområde.ARBEIDSGIVERREFUSJON,
                fagsystemId = FAGSYSTEMID,
                endringskode = SimuleringRequest.Oppdrag.Endringskode.ENDRET,
                mottakerAvUtbetalingen = ORGNR,
                linjer = listOf(
                    SimuleringRequest.Oppdrag.Oppdragslinje(
                        endringskode = SimuleringRequest.Oppdrag.Endringskode.NY,
                        fom = LocalDate.of(2018, 1, 1),
                        tom = LocalDate.of(2018, 1, 14),
                        satstype = SimuleringRequest.Oppdrag.Oppdragslinje.Satstype.DAGLIG,
                        sats = DAGSATS,
                        grad = GRAD,
                        delytelseId = 1,
                        refDelytelseId = null,
                        refFagsystemId = null,
                        klassekode = SimuleringRequest.Oppdrag.Oppdragslinje.Klassekode.REFUSJON_IKKE_OPPLYSNINGSPLIKTIG,
                        opphørerFom = null
                    ),
                    SimuleringRequest.Oppdrag.Oppdragslinje(
                        endringskode = SimuleringRequest.Oppdrag.Endringskode.NY,
                        fom = LocalDate.of(2018, 1, 15),
                        tom = LocalDate.of(2018, 1, 31),
                        satstype = SimuleringRequest.Oppdrag.Oppdragslinje.Satstype.DAGLIG,
                        sats = DAGSATS,
                        grad = GRAD,
                        delytelseId = 1,
                        refDelytelseId = null,
                        refFagsystemId = null,
                        klassekode = SimuleringRequest.Oppdrag.Oppdragslinje.Klassekode.REFUSJON_IKKE_OPPLYSNINGSPLIKTIG,
                        opphørerFom = null
                    )
                )
            ),
            maksdato = MAKSDATO,
            saksbehandler = SAKSBEHANDLER
        )

    private fun xmlResponse(body: String): String {
        @Language("XML")
        val response = """<?xml version='1.0' encoding='UTF-8'?>
<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
    <S:Body>$body</S:Body>
</S:Envelope>"""
        return response
    }


    private fun mockClient(response: String): Pair<HttpClient, Simuleringtjeneste> {
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
        return httpClient to Simuleringtjeneste(client)
    }
}