package no.nav.helse.spenn.simulering.api

import com.github.navikt.tbd_libs.mock.MockHttpResponse
import com.github.navikt.tbd_libs.result_object.Result
import com.github.navikt.tbd_libs.result_object.ok
import com.github.navikt.tbd_libs.soap.MinimalSoapClient
import com.github.navikt.tbd_libs.soap.SamlToken
import com.github.navikt.tbd_libs.soap.SamlTokenProvider
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.spenn.simulering.api.client.Detaljer
import no.nav.helse.spenn.simulering.api.client.Simulering
import no.nav.helse.spenn.simulering.api.client.SimuleringV2Service
import no.nav.helse.spenn.simulering.api.client.SimulertPeriode
import no.nav.helse.spenn.simulering.api.client.Utbetaling
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.time.LocalDate
import java.time.LocalDateTime

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
        val result =
            simuleringClient.simulerOppdrag(
                simulering = simulerRequest,
                serviceuserUsername = "en-serviceuser",
                serviceuserPassword = "et-passord",
            )
        assertInstanceOf<SimuleringResponse.Ok>(result)
    }

    @Test
    fun `tolker simuleringsresultat med desimaltall og flere detaljer`() {
        @Language("XML")
        val xml = """<simulerBeregningResponse xmlns="http://nav.no/system/os/tjenester/simulerFpService/simulerFpServiceGrensesnitt">
    <response xmlns="">
        <simulering>
            <gjelderId>12345678911</gjelderId>
            <gjelderNavn>NORMAL MUFFINS   </gjelderNavn>
            <datoBeregnet>2018-01-17</datoBeregnet>
            <kodeFaggruppe>KORTTID</kodeFaggruppe>
            <belop>4501.75</belop>
            <beregningsPeriode>
                <periodeFom>2018-01-01</periodeFom>
                <periodeTom>2018-01-31</periodeTom>
                <beregningStoppnivaa>
                    <kodeFagomraade>SPREF</kodeFagomraade>
                    <stoppNivaaId>1</stoppNivaaId>
                    <behandlendeEnhet>8020</behandlendeEnhet>
                    <oppdragsId>1</oppdragsId>
                    <fagsystemId>$FAGSYSTEMID  </fagsystemId>
                    <kid/>
                    <utbetalesTilId>00$ORGNR</utbetalesTilId>
                    <utbetalesTilNavn>EN ARBEIDSGIVER  </utbetalesTilNavn>
                    <bilagsType>U</bilagsType>
                    <forfall>2018-02-15</forfall>
                    <feilkonto>true</feilkonto>
                    <beregningStoppnivaaDetaljer>
                        <faktiskFom>2018-01-01</faktiskFom>
                        <faktiskTom>2018-01-14</faktiskTom>
                        <kontoStreng>1338011    </kontoStreng>
                        <behandlingskode>2</behandlingskode>
                        <belop>3001.50</belop>
                        <tilbakeforing>false</tilbakeforing>
                        <sats>1000.50</sats>
                        <typeSats>DAG </typeSats>
                        <antallSats>3.00</antallSats>
                        <uforeGrad>100</uforeGrad>
                        <klassekode>SPREFAG-IOP</klassekode>
                        <klasseKodeBeskrivelse>Sykepenger, Refusjon arbeidsgiver </klasseKodeBeskrivelse>
                        <typeKlasse>YTEL</typeKlasse>
                        <refunderesOrgNr>00$ORGNR</refunderesOrgNr>
                    </beregningStoppnivaaDetaljer>
                    <beregningStoppnivaaDetaljer>
                        <faktiskFom>2018-01-15</faktiskFom>
                        <faktiskTom>2018-01-31</faktiskTom>
                        <kontoStreng>1338011    </kontoStreng>
                        <behandlingskode>2</behandlingskode>
                        <belop>-1500.25</belop>
                        <tilbakeforing>true</tilbakeforing>
                        <sats>1500.25</sats>
                        <typeSats>DAG</typeSats>
                        <antallSats>1.00</antallSats>
                        <uforeGrad>50</uforeGrad>
                        <klassekode>SPREFAG-IOP</klassekode>
                        <klasseKodeBeskrivelse>Sykepenger, Refusjon arbeidsgiver</klasseKodeBeskrivelse>
                        <typeKlasse>YTEL</typeKlasse>
                        <refunderesOrgNr>00$ORGNR</refunderesOrgNr>
                    </beregningStoppnivaaDetaljer>
                </beregningStoppnivaa>
            </beregningsPeriode>
        </simulering>
    </response>
</simulerBeregningResponse>"""

        val (_, simuleringClient) = mockClient(xmlResponse(xml))
        val result =
            simuleringClient.simulerOppdrag(
                simulering = simuleringRequest(),
                serviceuserUsername = "en-serviceuser",
                serviceuserPassword = "et-passord",
            )

        val forventetDetalj =
            Detaljer(
                faktiskFom = LocalDate.of(2018, 1, 1),
                faktiskTom = LocalDate.of(2018, 1, 14),
                konto = "1338011",
                belop = 3001,
                tilbakeforing = false,
                sats = BigDecimal("1000.50").toDouble(),
                typeSats = "DAG",
                antallSats = 3,
                uforegrad = 100,
                klassekode = "SPREFAG-IOP",
                klassekodeBeskrivelse = "Sykepenger, Refusjon arbeidsgiver",
                utbetalingsType = "YTEL",
                refunderesOrgNr = ORGNR,
            )
        val forventet =
            Simulering(
                gjelderId = PERSON,
                gjelderNavn = "NORMAL MUFFINS",
                datoBeregnet = LocalDate.of(2018, 1, 17),
                totalBelop = 4501,
                periodeList =
                    listOf(
                        SimulertPeriode(
                            fom = LocalDate.of(2018, 1, 1),
                            tom = LocalDate.of(2018, 1, 31),
                            utbetaling =
                                listOf(
                                    Utbetaling(
                                        fagSystemId = FAGSYSTEMID,
                                        utbetalesTilId = ORGNR,
                                        utbetalesTilNavn = "EN ARBEIDSGIVER",
                                        forfall = LocalDate.of(2018, 2, 15),
                                        feilkonto = true,
                                        detaljer =
                                            listOf(
                                                forventetDetalj,
                                                forventetDetalj.copy(
                                                    faktiskFom = LocalDate.of(2018, 1, 15),
                                                    faktiskTom = LocalDate.of(2018, 1, 31),
                                                    belop = -1500,
                                                    tilbakeforing = true,
                                                    sats = BigDecimal("1500.25").toDouble(),
                                                    antallSats = 1,
                                                    uforegrad = 50,
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )
        assertEquals(SimuleringResponse.Ok(forventet), result)
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
        val result =
            simuleringClient.simulerOppdrag(
                simulering = simulerRequest,
                serviceuserUsername = "en-serviceuser",
                serviceuserPassword = "et-passord",
            )
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
        val result =
            simuleringClient.simulerOppdrag(
                simulering = simulerRequest,
                serviceuserUsername = "en-serviceuser",
                serviceuserPassword = "et-passord",
            )
        assertInstanceOf<SimuleringResponse.TekniskFeil>(result)
    }

    private fun simuleringRequest() =
        SimuleringRequest(
            fødselsnummer = PERSON,
            oppdrag =
                SimuleringRequest.Oppdrag(
                    fagområde = SimuleringRequest.Oppdrag.Fagområde.ARBEIDSGIVERREFUSJON,
                    fagsystemId = FAGSYSTEMID,
                    endringskode = SimuleringRequest.Oppdrag.Endringskode.ENDRET,
                    mottakerAvUtbetalingen = ORGNR,
                    linjer =
                        listOf(
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
                                klassekodeFom = LocalDate.of(2018, 1, 1),
                                opphørerFom = null,
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
                                klassekodeFom = LocalDate.of(2018, 1, 15),
                                opphørerFom = null,
                            ),
                        ),
                ),
            maksdato = MAKSDATO,
            saksbehandler = SAKSBEHANDLER,
        )

    private fun xmlResponse(body: String): String {
        @Language("XML")
        val response = """<?xml version='1.0' encoding='UTF-8'?>
<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
    <S:Body>$body</S:Body>
</S:Envelope>"""
        return response
    }

    private fun mockClient(
        response: String,
        statusCode: Int = 200,
    ): Pair<HttpClient, Simuleringtjeneste> {
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
                ): Result<SamlToken> = SamlToken("<saml token>", LocalDateTime.now().plusHours(1)).ok()
            }
        val soapClient = MinimalSoapClient(URI("http://simulering-ws"), tokenProvider, httpClient)
        val client = SimuleringV2Service(soapClient = soapClient)
        return httpClient to Simuleringtjeneste(client)
    }
}
