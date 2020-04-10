package no.nav.helse.spenn.avstemming

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.spenn.Avstemmingsnøkkel
import no.nav.helse.spenn.TestConnection
import no.nav.helse.spenn.januar
import no.nav.helse.spenn.utbetaling.OppdragDao
import no.nav.helse.spenn.utbetaling.OppdragDto
import no.nav.helse.spenn.utbetaling.Oppdragstatus.*
import org.apache.kafka.clients.producer.Producer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class AvstemmingTest {
    private companion object {
        private const val FAGOMRÅDE_REFUSJON = "SPREF"
        private const val PERSON = "12345678911"
        private const val ORGNR = "123456789"
        private const val UTBETALINGSREF = "f227ed9f-6b53-4db6-a921-bdffb8098bd3"
        private const val AVSTEMMINGSNØKKEL = 1L
        private const val SAKSBEHANDLER = "Navn Navnesen"
        private const val SEND_QUEUE = "avstemmingskø"
        private const val BELØP = 1000
        private val OPPRETTET = LocalDateTime.now()

        private val mandag = 1.januar.atStartOfDay()
        private val tirsdag = mandag.plusDays(1)
        private val onsdag = tirsdag.plusDays(1)
        private val torsdag = onsdag.plusDays(1)
        private val fredag = torsdag.plusDays(1)
        private val lørdag = fredag.plusDays(1)
        private val søndag = lørdag.plusDays(1)

        private const val OK = "00"
        private const val OK_MED_VARSEL = "04"
        private const val AVVIST_FUNKSJONELLE_FEIL = "08"
        private const val AVVIST_TEKNISK_FEIL = "12"
    }

    private val dao = mockk<OppdragDao>(relaxed = true)
    private val producer = mockk<Producer<String, String>>(relaxed = true)
    private val avstemmingDao = mockk<AvstemmingDao>(relaxed = true)
    private val connection = TestConnection()

    private lateinit var avstemming: Avstemming

    private val oppdrag = mapOf(
        FAGOMRÅDE_REFUSJON to listOf(
            OppdragDto(AVSTEMMINGSNØKKEL, PERSON, UTBETALINGSREF, OPPRETTET, OVERFØRT, BELØP, null),
            OppdragDto(AVSTEMMINGSNØKKEL + 1, PERSON, UTBETALINGSREF, mandag, AKSEPTERT, BELØP, kvittering(OK)),
            OppdragDto(AVSTEMMINGSNØKKEL + 2, PERSON, UTBETALINGSREF, tirsdag, AKSEPTERT, BELØP, kvittering(OK)),
            OppdragDto(AVSTEMMINGSNØKKEL + 3, PERSON, UTBETALINGSREF, onsdag, AKSEPTERT_MED_FEIL, BELØP, kvittering(OK_MED_VARSEL)),
            OppdragDto(AVSTEMMINGSNØKKEL + 4, PERSON, UTBETALINGSREF, torsdag, AVVIST, BELØP, kvittering(AVVIST_FUNKSJONELLE_FEIL)),
            OppdragDto(AVSTEMMINGSNØKKEL + 5, PERSON, UTBETALINGSREF, fredag, AVVIST, -BELØP, kvittering(AVVIST_FUNKSJONELLE_FEIL)),
            OppdragDto(AVSTEMMINGSNØKKEL + 6, PERSON, UTBETALINGSREF, lørdag, AVVIST, -BELØP, kvittering(AVVIST_TEKNISK_FEIL)),
            OppdragDto(AVSTEMMINGSNØKKEL + 7, PERSON, UTBETALINGSREF, søndag, FEIL, BELØP, kvittering(OK))
        )
    )

    @Test
    fun avstem() {
        val dagen = LocalDate.now()
        val avstemmingsperiodeForDagen = Avstemmingsnøkkel.periode(dagen)
        val avstemmingsperiode = OppdragDto.avstemmingsperiode(oppdrag.getValue(FAGOMRÅDE_REFUSJON))
        every { dao.hentOppdragForAvstemming(avstemmingsperiodeForDagen.endInclusive) } returns oppdrag
        avstemming.avstem(dagen)
        assertEquals(3, connection.inspektør.antall())
        verify(exactly = 1) { avstemmingDao.nyAvstemming(any(), FAGOMRÅDE_REFUSJON, avstemmingsperiode.endInclusive, oppdrag.getValue(FAGOMRÅDE_REFUSJON).size) }
        verify(exactly = 1) { dao.oppdaterAvstemteOppdrag(FAGOMRÅDE_REFUSJON, avstemmingsperiode.endInclusive) }
        verify(exactly = 1) { producer.send(any()) }
    }

    @Test
    fun `ingenting å avstemme`() {
        val dagen = LocalDate.now()
        val avstemmingsperiode = Avstemmingsnøkkel.periode(dagen)
        every { dao.hentOppdragForAvstemming(avstemmingsperiode.endInclusive) } returns emptyMap()
        avstemming.avstem(dagen)
        assertEquals(0, connection.inspektør.antall())
        verify(exactly = 0) { avstemmingDao.nyAvstemming(any(), any(), any(), any()) }
        verify(exactly = 0) { dao.oppdaterAvstemteOppdrag(any(), any()) }
        verify(exactly = 1) { producer.send(any()) }
    }

    @BeforeEach
    fun clear() {
        clearAllMocks()
        connection.reset()
        avstemming = Avstemming(connection, SEND_QUEUE, producer, "rapidTopic", dao, avstemmingDao)
    }

    private fun kvittering(alvorlighetsgrad: String) = """<?xml version="1.0" encoding="utf-8"?>
<ns2:oppdrag xmlns:ns2="http://www.trygdeetaten.no/skjema/oppdrag">
    <mmel>
        <systemId>231-OPPD</systemId>
        <alvorlighetsgrad>$alvorlighetsgrad</alvorlighetsgrad>
    </mmel>
    <oppdrag-110>
        <kodeAksjon>1</kodeAksjon>
        <kodeEndring>NY</kodeEndring>
        <kodeFagomraade>SPREF</kodeFagomraade>
        <fagsystemId>$UTBETALINGSREF</fagsystemId>
        <utbetFrekvens>MND</utbetFrekvens>
        <oppdragGjelderId>$PERSON</oppdragGjelderId>
        <datoOppdragGjelderFom>1970-01-01+01:00</datoOppdragGjelderFom>
        <saksbehId>$SAKSBEHANDLER</saksbehId>
        <avstemming-115>
            <kodeKomponent>SP</kodeKomponent>
            <nokkelAvstemming>$AVSTEMMINGSNØKKEL</nokkelAvstemming>
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
            <saksbehId>$SAKSBEHANDLER</saksbehId>
            <refusjonsinfo-156>
                <maksDato>2020-09-20+02:00</maksDato>
                <refunderesId>$ORGNR</refunderesId>
                <datoFom>2019-01-01+01:00</datoFom>
            </refusjonsinfo-156>
            <grad-170>
                <typeGrad>UFOR</typeGrad>
                <grad>50</grad>
            </grad-170>
            <attestant-180>
                <attestantId>$SAKSBEHANDLER</attestantId>
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
            <saksbehId>$SAKSBEHANDLER</saksbehId>
            <refusjonsinfo-156>
                <maksDato>2020-09-20+02:00</maksDato>
                <refunderesId>$ORGNR</refunderesId>
                <datoFom>2019-02-13+01:00</datoFom>
            </refusjonsinfo-156>
            <grad-170>
                <typeGrad>UFOR</typeGrad>
                <grad>70</grad>
            </grad-170>
            <attestant-180>
                <attestantId>$SAKSBEHANDLER</attestantId>
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
            <saksbehId>$SAKSBEHANDLER</saksbehId>
            <refusjonsinfo-156>
                <maksDato>2020-09-20+02:00</maksDato>
                <refunderesId>$ORGNR</refunderesId>
                <datoFom>2019-03-18+01:00</datoFom>
            </refusjonsinfo-156>
            <grad-170>
                <typeGrad>UFOR</typeGrad>
                <grad>100</grad>
            </grad-170>
            <attestant-180>
                <attestantId>$SAKSBEHANDLER</attestantId>
            </attestant-180>
        </oppdrags-linje-150>
    </oppdrag-110>
</ns2:oppdrag>"""
}
