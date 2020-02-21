package no.nav.helse.spenn.oppdrag


import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.ibm.mq.jms.MQQueue
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spenn.*
import no.nav.helse.spenn.UtbetalingLøser.Companion.lagOppdragFraBehov
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import no.nav.helse.spenn.testsupport.TestDb
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import java.time.LocalDateTime
import javax.jms.Connection
import javax.jms.MessageConsumer
import javax.jms.Session
import kotlin.test.assertEquals
import kotlin.test.assertNull

private data class TransRec(val status: String, val feilbeskrivelse: String?)

internal class MQOppdragReceiverTest {

    private val dataSource = TestDb.createMigratedDataSource()
    private val oppdragService = OppdragService(dataSource)

    private val meterRegistry = CompositeMeterRegistry()

    private val mockConnection = mock(Connection::class.java)
    private val mockJmsSession = mock(Session::class.java)
    private val mockConsumer = mock(MessageConsumer::class.java)
    private val mockRapidConnection = mock(RapidsConnection::class.java)

    @BeforeEach
    fun beforeEach() {
        kWhen(mockConnection.createSession()).thenReturn(mockJmsSession)
        kWhen(mockJmsSession.createConsumer(MQQueue("mottaksqueue"))).thenReturn(mockConsumer)
    }

    @Test
    fun `Oppdrag MQ send and receive`() {
        val mqReceiver = OppdragMQReceiver(
            connection = mockConnection,
            rapidsConnection = mockRapidConnection,
            mottakqueue = "mottaksqueue",
            jaxb = JAXBOppdrag(), oppdragService = oppdragService,
            meterRegistry = meterRegistry
        )

        val etEnkeltBehov = etEnkeltBehov(utbetalingsreferanse = "3001")

        val oppdrag = lagOppdragFraBehov(etEnkeltBehov.toOppdragsbehov())

        oppdragService.lagreNyttOppdrag(oppdrag)
        oppdragService.hentNyeOppdrag(5).first().forberedSendingTilOS()

        val nokkel = LocalDateTime.parse("2019-09-20-13.31.28.572227", avstemmingsnokkelFormatter)
        overstyrNokkelForUtbetalingsreferanse("3001", nokkel)

        mqReceiver.receiveOppdragResponse(receiveError)
        verify(mockRapidConnection).publish(
            "12345678901", etEnkeltBehov.setLøsning(
            "Utbetaling",
            mapOf(
                "status" to TransaksjonStatus.FEIL,
                "melding" to "Det er noe galt"
            )
        ).toString())

        val feilTransaksjon = hentTransaksjonerMedUtbetalingsreferanse("3001").first()
        assertEquals(TransaksjonStatus.FEIL.name, feilTransaksjon.status)
        assertEquals("Det er noe galt", feilTransaksjon.feilbeskrivelse)

        mqReceiver.receiveOppdragResponse(receiveOK)
        verify(mockRapidConnection).publish("12345678901", etEnkeltBehov.setLøsning(
            "Utbetaling",
            mapOf(
                "status" to TransaksjonStatus.FERDIG,
                "melding" to ""
            )
        ).toString())

        val okTransaksjon = hentTransaksjonerMedUtbetalingsreferanse("3001").first()
        assertEquals(TransaksjonStatus.FERDIG.name, okTransaksjon.status)
        assertNull(okTransaksjon.feilbeskrivelse)
    }

    private fun overstyrNokkelForUtbetalingsreferanse(referanse: String, nokkel: LocalDateTime) {
        dataSource.connection.use {
            it.prepareStatement(
                """
                    update transaksjon set nokkel = ? where oppdrag_id in (select id from oppdrag where utbetalingsreferanse = ?)
                """.trimIndent()
            ).use { preparedStatement ->
                preparedStatement.setObject(1, nokkel)
                preparedStatement.setString(2, referanse)
                preparedStatement.executeUpdate()
            }
        }
    }

    private fun hentTransaksjonerMedUtbetalingsreferanse(referanse: String): List<TransRec> {
        dataSource.connection.use {
            it.prepareStatement(
                """
                    select transaksjon.id as transaksjon_id, utbetalingsreferanse, status, feilbeskrivelse
                    from oppdrag join transaksjon on oppdrag.id = transaksjon.oppdrag_id
                    where utbetalingsreferanse = ?
                """.trimIndent()
            ).use { preparedStatement ->
                preparedStatement.setString(1, referanse)
                preparedStatement.executeQuery().use { resultSet ->
                    val result = mutableListOf<TransRec>()
                    while (resultSet.next()) {
                        result.add(TransRec(resultSet.getString("status"), resultSet.getString("feilbeskrivelse")))
                    }
                    return result.toList()
                }
            }
        }
    }
}

private fun JsonNode.setLøsning(nøkkel: String, data: Any) =
    (this as ObjectNode).set<JsonNode>(
        "@løsning", defaultObjectMapper.convertValue(
        mapOf(
            nøkkel to data
        ))
    )


const val receiveError = """<?xml version="1.0" encoding="utf-8"?>
<oppdrag xmlns="http://www.trygdeetaten.no/skjema/oppdrag"><mmel>
<systemId>231-OPPD</systemId>
<kodeMelding>B100022F</kodeMelding>
<alvorlighetsgrad>08</alvorlighetsgrad>
<beskrMelding>Det er noe galt</beskrMelding>
<programId>K231BB00</programId>
<sectionNavn>D115-BEHAND-AVST-NOKLER</sectionNavn>
</mmel><oppdrag-110>
<kodeAksjon>1</kodeAksjon>
<kodeEndring>
    NY
</kodeEndring>
<kodeFagomraade>SPREF</kodeFagomraade>
<fagsystemId>3001</fagsystemId>
<utbetFrekvens>MND</utbetFrekvens>
<oppdragGjelderId>
    11111111111
</oppdragGjelderId>
<datoOppdragGjelderFom>
    1970-01-01+01:00
</datoOppdragGjelderFom>
<saksbehId>SPA</saksbehId>
<avstemming-115>
    <kodeKomponent>SP</kodeKomponent>
    <nokkelAvstemming>2019-09-20-13.31.28.572227</nokkelAvstemming>
    <tidspktMelding>2019-09-20-13.31.28.572227</tidspktMelding>
</avstemming-115>

<oppdrags-enhet-120>
    <typeEnhet>BOS</typeEnhet>
    <enhet>
        4151
    </enhet>
    <datoEnhetFom>1970-01-01+01:00</datoEnhetFom>
</oppdrags-enhet-120>

<oppdrags-linje-150>
    <kodeEndringLinje>NY</kodeEndringLinje>
    <delytelseId>1</delytelseId>
    <kodeKlassifik>
        SPREFAG-IOP
    </kodeKlassifik>
    <datoVedtakFom>
        2019-01-01+01:00
    </datoVedtakFom>
    <datoVedtakTom>2019-01-12+01:00</datoVedtakTom>
    <sats>
        600
    </sats>
    <fradragTillegg>T</fradragTillegg>
    <typeSats>
        DAG
    </typeSats>
    <brukKjoreplan>N</brukKjoreplan>
    <saksbehId>SPA</saksbehId>
    <refusjonsinfo-156>
        <maksDato>2020-09-19+02:00</maksDato>
        <refunderesId>
            00995816598
        </refunderesId>
        <datoFom>2019-01-01+01:00</datoFom>
    </refusjonsinfo-156>

    <grad-170>
        <typeGrad>UFOR</typeGrad>
        <grad>50</grad>
    </grad-170>

    <attestant-180>
        <attestantId>SPA</attestantId>
    </attestant-180>

</oppdrags-linje-150>

<oppdrags-linje-150>
    <kodeEndringLinje>NY</kodeEndringLinje>
    <delytelseId>2</delytelseId>
    <kodeKlassifik>
        SPREFAG-IOP
    </kodeKlassifik>
    <datoVedtakFom>
        2019-02-13+01:00
    </datoVedtakFom>
    <datoVedtakTom>2019-02-20+01:00</datoVedtakTom>
    <sats>
        600
    </sats>
    <fradragTillegg>T</fradragTillegg>
    <typeSats>
        DAG
    </typeSats>
    <brukKjoreplan>N</brukKjoreplan>
    <saksbehId>SPA</saksbehId>
    <refusjonsinfo-156>
        <maksDato>2020-09-19+02:00</maksDato>
        <refunderesId>
            00995816598
        </refunderesId>
        <datoFom>2019-02-13+01:00</datoFom>
    </refusjonsinfo-156>

    <grad-170>
        <typeGrad>UFOR</typeGrad>
        <grad>70</grad>
    </grad-170>

    <attestant-180>
        <attestantId>SPA</attestantId>
    </attestant-180>

</oppdrags-linje-150>

<oppdrags-linje-150>
    <kodeEndringLinje>NY</kodeEndringLinje>
    <delytelseId>3</delytelseId>
    <kodeKlassifik>
        SPREFAG-IOP
    </kodeKlassifik>
    <datoVedtakFom>
        2019-03-18+01:00
    </datoVedtakFom>
    <datoVedtakTom>2019-04-12+02:00</datoVedtakTom>
    <sats>
        1000
    </sats>
    <fradragTillegg>T</fradragTillegg>
    <typeSats>
        DAG
    </typeSats>
    <brukKjoreplan>N</brukKjoreplan>
    <saksbehId>SPA</saksbehId>
    <refusjonsinfo-156>
        <maksDato>2020-09-19+02:00</maksDato>
        <refunderesId>
            00995816598
        </refunderesId>
        <datoFom>2019-03-18+01:00</datoFom>
    </refusjonsinfo-156>

    <grad-170>
        <typeGrad>UFOR</typeGrad>
        <grad>100</grad>
    </grad-170>

    <attestant-180>
        <attestantId>SPA</attestantId>
    </attestant-180>

</oppdrags-linje-150>

</oppdrag-110></ns2:oppdrag>
"""

const val receiveOK = """<?xml version="1.0" encoding="utf-8"?>
<oppdrag xmlns="http://www.trygdeetaten.no/skjema/oppdrag">
    <mmel>
        <systemId>231-OPPD</systemId>
        <alvorlighetsgrad>00</alvorlighetsgrad>
    </mmel>
    <oppdrag-110>
        <kodeAksjon>1</kodeAksjon>
        <kodeEndring>NY</kodeEndring>
        <kodeFagomraade>SPREF</kodeFagomraade>
        <fagsystemId>3001</fagsystemId>
        <utbetFrekvens>MND</utbetFrekvens>
        <oppdragGjelderId>11111111111</oppdragGjelderId>
        <datoOppdragGjelderFom>1970-01-01+01:00</datoOppdragGjelderFom>
        <saksbehId>SPA</saksbehId>
        <avstemming-115>
            <kodeKomponent>SP</kodeKomponent>
            <nokkelAvstemming>2019-09-20-13.31.28.572227</nokkelAvstemming>
            <tidspktMelding>2019-09-20-13.31.28.572227</tidspktMelding>
        </avstemming-115>
        <oppdrags-enhet-120>
            <typeEnhet>BOS</typeEnhet>
            <enhet>4151</enhet>
            <datoEnhetFom>1970-01-01+01:00</datoEnhetFom>
        </oppdrags-enhet-120>
        <oppdrags-linje-150>
            <kodeEndringLinje>NY</kodeEndringLinje>
            <delytelseId>1</delytelseId>
            <kodeKlassifik>SPREFAG-IOP</kodeKlassifik>
            <datoVedtakFom>2019-01-01+01:00</datoVedtakFom>
            <datoVedtakTom>2019-01-12+01:00</datoVedtakTom>
            <sats>600</sats>
            <fradragTillegg>T</fradragTillegg>
            <typeSats>DAG</typeSats>
            <brukKjoreplan>N</brukKjoreplan>
            <saksbehId>SPA</saksbehId>
            <refusjonsinfo-156>
                <maksDato>2020-09-20+02:00</maksDato>
                <refunderesId>00995816598</refunderesId>
                <datoFom>2019-01-01+01:00</datoFom>
            </refusjonsinfo-156>
            <grad-170>
                <typeGrad>UFOR</typeGrad>
                <grad>50</grad>
            </grad-170>
            <attestant-180>
                <attestantId>SPA</attestantId>
            </attestant-180>
        </oppdrags-linje-150>
        <oppdrags-linje-150>
            <kodeEndringLinje>NY</kodeEndringLinje>
            <delytelseId>2</delytelseId>
            <kodeKlassifik>SPREFAG-IOP</kodeKlassifik>
            <datoVedtakFom>2019-02-13+01:00</datoVedtakFom>
            <datoVedtakTom>2019-02-20+01:00</datoVedtakTom>
            <sats>600</sats>
            <fradragTillegg>T</fradragTillegg>
            <typeSats>DAG</typeSats>
            <brukKjoreplan>N</brukKjoreplan>
            <saksbehId>SPA</saksbehId>
            <refusjonsinfo-156>
                <maksDato>2020-09-20+02:00</maksDato>
                <refunderesId>00995816598</refunderesId>
                <datoFom>2019-02-13+01:00</datoFom>
            </refusjonsinfo-156>
            <grad-170>
                <typeGrad>UFOR</typeGrad>
                <grad>70</grad>
            </grad-170>
            <attestant-180>
                <attestantId>SPA</attestantId>
            </attestant-180>
        </oppdrags-linje-150>
        <oppdrags-linje-150>
            <kodeEndringLinje>NY</kodeEndringLinje>
            <delytelseId>3</delytelseId>
            <kodeKlassifik>SPREFAG-IOP</kodeKlassifik>
            <datoVedtakFom>2019-03-18+01:00</datoVedtakFom>
            <datoVedtakTom>2019-04-12+02:00</datoVedtakTom>
            <sats>1000</sats>
            <fradragTillegg>T</fradragTillegg>
            <typeSats>DAG</typeSats>
            <brukKjoreplan>N</brukKjoreplan>
            <saksbehId>SPA</saksbehId>
            <refusjonsinfo-156>
                <maksDato>2020-09-20+02:00</maksDato>
                <refunderesId>00995816598</refunderesId>
                <datoFom>2019-03-18+01:00</datoFom>
            </refusjonsinfo-156>
            <grad-170>
                <typeGrad>UFOR</typeGrad>
                <grad>100</grad>
            </grad-170>
            <attestant-180>
                <attestantId>SPA</attestantId>
            </attestant-180>
        </oppdrags-linje-150>
    </oppdrag-110>
</ns2:oppdrag>"""
