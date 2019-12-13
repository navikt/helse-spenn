package no.nav.helse.spenn.oppdrag


import com.ibm.mq.jms.MQQueue
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import no.nav.helse.spenn.avstemmingsnokkelFormatter
import no.nav.helse.spenn.etEnkeltBehov
import no.nav.helse.spenn.kWhen
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import no.nav.helse.spenn.testsupport.TestDb
import no.nav.helse.spenn.vedtak.SpennOppdragFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.util.*
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

    @BeforeEach
    fun beforeEach() {
        kWhen(mockConnection.createSession()).thenReturn(mockJmsSession)
        kWhen(mockJmsSession.createConsumer(MQQueue("mottaksqueue"))).thenReturn(mockConsumer)
    }

    @Test
    fun OppdragMQSendAndReceiveTest() {
        val mqReceiver = OppdragMQReceiver(
            connection = mockConnection,
            mottakqueue = "mottaksqueue",
            jaxb = JAXBOppdrag(), oppdragService = oppdragService,
            meterRegistry = meterRegistry
        ) //, statusProducer = kafkaProducer)
        val fom1 = LocalDate.of(2019, Month.JANUARY, 1)
        val tom1 = LocalDate.of(2019, Month.JANUARY, 12)
        val oppdragslinje1 = UtbetalingsLinje(
            id = "1", datoFom = fom1,
            datoTom = tom1, sats = BigDecimal.valueOf(600), satsTypeKode = SatsTypeKode.DAGLIG,
            utbetalesTil = "995816598", grad = BigInteger.valueOf(50)
        )

        val fom2 = LocalDate.of(2019, Month.FEBRUARY, 13)
        val tom2 = LocalDate.of(2019, Month.FEBRUARY, 20)
        val oppdragslinje2 = UtbetalingsLinje(
            id = "2", datoFom = fom2,
            datoTom = tom2, sats = BigDecimal.valueOf(600), satsTypeKode = SatsTypeKode.DAGLIG,
            utbetalesTil = "995816598", grad = BigInteger.valueOf(70)
        )

        val fom3 = LocalDate.of(2019, Month.MARCH, 18)
        val tom3 = LocalDate.of(2019, Month.APRIL, 12)
        val oppdragslinje3 = UtbetalingsLinje(
            id = "3", datoFom = fom3,
            datoTom = tom3, sats = BigDecimal.valueOf(1000), satsTypeKode = SatsTypeKode.DAGLIG,
            utbetalesTil = "995816598", grad = BigInteger.valueOf(100)
        )

        val utbetalingTemplate =
            SpennOppdragFactory.lagOppdragFraBehov(etEnkeltBehov(), "11111111111")
        val utbetaling = utbetalingTemplate.copy(
            utbetaling = utbetalingTemplate.utbetaling!!.copy(
                utbetalingsLinjer = listOf(oppdragslinje1, oppdragslinje2, oppdragslinje3)
            )
        )

        val uuid = UUID.randomUUID()
        val oppdrag =
            utbetaling.copy(utbetalingsreferanse = "3001")
        oppdragService.lagreNyttOppdrag(oppdrag)
        oppdragService.hentNyeOppdrag(5).first().forberedSendingTilOS()

        val nokkel = LocalDateTime.parse("2019-09-20-13.31.28.572227", avstemmingsnokkelFormatter)
        overstyrNokkelForUtbetalingsreferanse("3001", nokkel)

        mqReceiver.receiveOppdragResponse(receiveError)
        val feilTransaksjon = hentTransaksjonerMedUtbetalingsreferanse("3001").first()
        assertEquals(TransaksjonStatus.FEIL.name, feilTransaksjon.status)
        assertEquals("Det er noe galt", feilTransaksjon.feilbeskrivelse)

        mqReceiver.receiveOppdragResponse(receiveOK)
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