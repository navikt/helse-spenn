package no.nav.helse.spenn.oppdrag

import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import io.micrometer.core.instrument.cumulative.CumulativeCounter
import no.nav.helse.spenn.appsupport.OPPDRAG
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
import no.nav.helse.spenn.vedtak.Vedtak
import org.junit.jupiter.api.Test


import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.ComponentScan
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.Month
import java.util.*
import kotlin.test.assertEquals

@JooqTest(properties = ["VAULT_ENABLED=false",
    "spring.cloud.vault.enabled=false",
    "spring.test.database.replace=none"])
@ComponentScan(basePackages = ["no.nav.helse.spenn.oppdrag.dao"])
class MQOppdragReceiverTest {

    @Autowired
    lateinit var oppdragStateService: OppdragStateService

    val meterRegistry = CompositeMeterRegistry()
    val kafkaProducer = mock(OppdragStateKafkaProducer::class.java)

    @Test
    fun OppdragMQSendAndReceiveTest() {
        val mqReceiver = OppdragMQReceiver(jaxb = JAXBOppdrag(), oppdragStateService = oppdragStateService,
                meterRegistry = meterRegistry, statusProducer = kafkaProducer)
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
                oppdragGjelder = "21038014495", utbetalingsLinje = listOf(oppdragslinje1, oppdragslinje2, oppdragslinje3),
                vedtak = Vedtak(
                        soknadId = UUID.randomUUID(),
                        maksDato = LocalDate.now().plusYears(1),
                        aktorId = "12341234",
                        vedtaksperioder = emptyList()
                ))
        val uuid = "ZApp5GWKTB6XtoaYYtHBOg".fromFagId()
        val oppdragState = OppdragStateDTO(soknadId = uuid,
                utbetalingsOppdrag = utbetaling)
        val state = oppdragStateService.saveOppdragState(oppdragState)
        val oppdrag = mqReceiver.receiveOppdragResponse(receiveError)
        assertEquals(OppdragStateStatus.FEIL, oppdrag.status)
        assertEquals("Mangler verdi i Avst-nøkkel på id-115", oppdrag.feilbeskrivelse)
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
<fagsystemId>ZApp5GWKTB6XtoaYYtHBOg</fagsystemId>
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
val receiveOK=""" """