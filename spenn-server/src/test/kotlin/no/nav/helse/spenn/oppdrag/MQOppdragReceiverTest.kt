package no.nav.helse.spenn.oppdrag


import com.ibm.mq.jms.MQQueue
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import no.nav.helse.spenn.etEnkeltBehov
import no.nav.helse.spenn.kWhen
import no.nav.helse.spenn.oppdrag.dao.OppdragStateJooqRepository
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
import no.nav.helse.spenn.testsupport.TestDb
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.Month
import java.util.*
import javax.jms.Connection
import javax.jms.MessageConsumer
import javax.jms.Session
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MQOppdragReceiverTest {

    val oppdragStateService = OppdragStateService(
            OppdragStateJooqRepository(TestDb.createMigratedDSLContext())
    )

    val meterRegistry = CompositeMeterRegistry()

    val mockConnection = mock(Connection::class.java)
    val mockJmsSession = mock(Session::class.java)
    val mockConsumer = mock(MessageConsumer::class.java)

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
                jaxb = JAXBOppdrag(), oppdragStateService = oppdragStateService,
                meterRegistry = meterRegistry) //, statusProducer = kafkaProducer)
        val fom1 = LocalDate.of(2019, Month.JANUARY, 1)
        val tom1 = LocalDate.of(2019, Month.JANUARY, 12)
        val oppdragslinje1 = UtbetalingsLinje(id = "1", datoFom = fom1,
                datoTom =tom1, sats = BigDecimal.valueOf(600), satsTypeKode = SatsTypeKode.DAGLIG,
                utbetalesTil = "995816598", grad = BigInteger.valueOf(50))

        val fom2 = LocalDate.of(2019, Month.FEBRUARY, 13)
        val tom2 = LocalDate.of(2019, Month.FEBRUARY, 20)
        val oppdragslinje2 = UtbetalingsLinje(id = "2", datoFom = fom2,
                datoTom = tom2, sats = BigDecimal.valueOf(600), satsTypeKode = SatsTypeKode.DAGLIG,
                utbetalesTil = "995816598", grad = BigInteger.valueOf(70))

        val fom3 = LocalDate.of(2019, Month.MARCH, 18)
        val tom3 = LocalDate.of(2019, Month.APRIL, 12)
        val oppdragslinje3 = UtbetalingsLinje(id = "3", datoFom = fom3,
                datoTom = tom3, sats = BigDecimal.valueOf(1000), satsTypeKode = SatsTypeKode.DAGLIG,
                utbetalesTil = "995816598", grad = BigInteger.valueOf(100))

        val utbetaling = UtbetalingsOppdrag(operasjon = AksjonsKode.OPPDATER,
                oppdragGjelder = "11111111111", utbetalingsLinje = listOf(oppdragslinje1, oppdragslinje2, oppdragslinje3),
                behov = etEnkeltBehov()
        )
        val uuid = UUID.randomUUID()
        val oppdragState = TransaksjonDTO(
            sakskompleksId = uuid,
            utbetalingsreferanse = "3001",
            utbetalingsOppdrag = utbetaling
        )
        val ignored = oppdragStateService.saveOppdragState(oppdragState)
        val oppdrag = mqReceiver.receiveOppdragResponse(receiveError)
        assertEquals(OppdragStateStatus.FEIL, oppdrag.status)
        assertEquals("Mangler verdi i Avst-nøkkel på id-115", oppdrag.feilbeskrivelse)
        val ok = mqReceiver.receiveOppdragResponse(receiveOK)
        assertEquals(OppdragStateStatus.FERDIG, ok.status)
        assertNull(ok.feilbeskrivelse)
    }

}

val receiveError="""<?xml version="1.0" encoding="utf-8"?>
<oppdrag xmlns="http://www.trygdeetaten.no/skjema/oppdrag"><mmel>
<systemId>231-OPPD</systemId>
<kodeMelding>B100022F</kodeMelding>
<alvorlighetsgrad>08</alvorlighetsgrad>
<beskrMelding>Mangler verdi i Avst-nøkkel på id-115</beskrMelding>
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
val receiveOK="""<?xml version="1.0" encoding="utf-8"?>
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