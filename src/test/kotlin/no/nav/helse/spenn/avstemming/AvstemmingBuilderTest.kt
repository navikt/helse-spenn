package no.nav.helse.spenn.avstemming

import no.nav.helse.spenn.utbetaling.OppdragDto
import no.nav.helse.spenn.utbetaling.Oppdragstatus
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.AksjonType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class AvstemmingBuilderTest {
    private companion object {
        private const val PERSON = "12345678911"
        private const val ORGNR = "123456789"
        private const val UTBETALINGSREF = "f227ed9f-6b53-4db6-a921-bdffb8098bd3"
        private const val SAKSBEHANDLER = "Navn Navnesen"
        private const val AVSTEMMINGSNØKKEL = 1L
        private const val BELØP = 1000
        private val OPPRETTET = LocalDateTime.now()
        private const val DETALJER_PER_AVSTEMMINGMELDING = 5

        private const val AKSEPTERT_UTEN_FEIL = "00"
        private const val AKSEPTERT_MED_FEIL = "04"
        private const val AVVIST_FUNKSJONELLE_FEIL = "08"
        private const val AVVIST_TEKNISK_FEIL = "12"
        private const val UGYLDIG_FEILKODE = "??"
    }

    private val id = UUID.randomUUID()
    private val oppdrag = listOf(
        OppdragDto(
            AVSTEMMINGSNØKKEL,
            PERSON,
            UTBETALINGSREF,
            OPPRETTET,
            Oppdragstatus.OVERFØRT,
            BELØP,
            null
        ),
        OppdragDto(
            AVSTEMMINGSNØKKEL + 1,
            PERSON,
            UTBETALINGSREF,
            OPPRETTET,
            Oppdragstatus.AKSEPTERT,
            BELØP,
            kvittering(AKSEPTERT_UTEN_FEIL)
        ),
        OppdragDto(
            AVSTEMMINGSNØKKEL + 2,
            PERSON,
            UTBETALINGSREF,
            OPPRETTET.plusDays(1),
            Oppdragstatus.AKSEPTERT,
            BELØP,
            kvittering(AKSEPTERT_UTEN_FEIL)
        ),
        OppdragDto(
            AVSTEMMINGSNØKKEL + 3,
            PERSON,
            UTBETALINGSREF,
            OPPRETTET.plusDays(2),
            Oppdragstatus.AKSEPTERT_MED_FEIL,
            BELØP,
            kvittering(AKSEPTERT_MED_FEIL)
        ),
        OppdragDto(
            AVSTEMMINGSNØKKEL + 4,
            PERSON,
            UTBETALINGSREF,
            OPPRETTET.plusDays(3),
            Oppdragstatus.AVVIST,
            BELØP,
            kvittering(AVVIST_FUNKSJONELLE_FEIL)
        ),
        OppdragDto(
            AVSTEMMINGSNØKKEL + 5,
            PERSON,
            UTBETALINGSREF,
            OPPRETTET.plusDays(4),
            Oppdragstatus.AVVIST,
            -BELØP,
            kvittering(AVVIST_FUNKSJONELLE_FEIL)
        ),
        OppdragDto(
            AVSTEMMINGSNØKKEL + 6,
            PERSON,
            UTBETALINGSREF,
            OPPRETTET.plusDays(5),
            Oppdragstatus.AVVIST,
            -BELØP,
            kvittering(AVVIST_TEKNISK_FEIL)
        ),
        OppdragDto(
            AVSTEMMINGSNØKKEL + 7,
            PERSON,
            UTBETALINGSREF,
            OPPRETTET.plusDays(6),
            Oppdragstatus.FEIL,
            BELØP,
            kvittering(AKSEPTERT_UTEN_FEIL)
        )
    )
    private val detaljer = OppdragDto.detaljer(oppdrag)

    private lateinit var builder: AvstemmingBuilder

    @BeforeEach
    fun setup() {
        builder = AvstemmingBuilder(
            id,
            "SPREF",
            oppdrag,
            DETALJER_PER_AVSTEMMINGMELDING
        )
    }

    @Test
    fun `lager avstemmingsmeldinger`() {
        builder.build().also {
            assertEquals(4, it.size)
            assertAvstemmingdata(AksjonType.START, it.first())
            assertFørsteAvstemmingdataData(it[1])
            assertPåfølgendeAvstemmingdataData(detaljer.size - DETALJER_PER_AVSTEMMINGMELDING, it[2])
            assertAvstemmingdata(AksjonType.AVSL, it.last())
        }
    }

    private fun assertFørsteAvstemmingdataData(avstemmingsdata: Avstemmingsdata) {
        assertAvstemmingdataData(DETALJER_PER_AVSTEMMINGMELDING, avstemmingsdata)
        OppdragDto.totaldata(oppdrag).also {
            assertEquals(it.totalAntall, avstemmingsdata.total.totalAntall)
            assertEquals(it.totalBelop, avstemmingsdata.total.totalBelop)
            assertEquals(it.fortegn, avstemmingsdata.total.fortegn)
        }
        OppdragDto.grunnlagsdata(oppdrag).also {
            assertEquals(it.godkjentAntall, avstemmingsdata.grunnlag.godkjentAntall)
            assertEquals(it.godkjentBelop, avstemmingsdata.grunnlag.godkjentBelop)
            assertEquals(it.godkjentFortegn, avstemmingsdata.grunnlag.godkjentFortegn)
            assertEquals(it.varselAntall, avstemmingsdata.grunnlag.varselAntall)
            assertEquals(it.varselBelop, avstemmingsdata.grunnlag.varselBelop)
            assertEquals(it.varselFortegn, avstemmingsdata.grunnlag.varselFortegn)
            assertEquals(it.avvistAntall, avstemmingsdata.grunnlag.avvistAntall)
            assertEquals(it.avvistBelop, avstemmingsdata.grunnlag.avvistBelop)
            assertEquals(it.avvistFortegn, avstemmingsdata.grunnlag.avvistFortegn)
            assertEquals(it.manglerAntall, avstemmingsdata.grunnlag.manglerAntall)
            assertEquals(it.manglerBelop, avstemmingsdata.grunnlag.manglerBelop)
            assertEquals(it.manglerFortegn, avstemmingsdata.grunnlag.manglerFortegn)
        }
        assertNotNull(avstemmingsdata.periode.datoAvstemtFom)
        assertNotNull(avstemmingsdata.periode.datoAvstemtTom)
    }

    private fun assertPåfølgendeAvstemmingdataData(antall: Int, avstemmingsdata: Avstemmingsdata) {
        assertAvstemmingdataData(antall, avstemmingsdata)
        assertNull(avstemmingsdata.total)
        assertNull(avstemmingsdata.grunnlag)
        assertNull(avstemmingsdata.periode)
    }

    private fun assertAvstemmingdataData(antall: Int, avstemmingsdata: Avstemmingsdata) {
        assertAvstemmingdata(AksjonType.DATA, avstemmingsdata)
        assertEquals(antall, avstemmingsdata.detalj.size)
    }

    private fun assertAvstemmingdata(aksjonstype: AksjonType, avstemmingsdata: Avstemmingsdata) {
        assertEquals(aksjonstype, avstemmingsdata.aksjon.aksjonType)
        assertEquals("$AVSTEMMINGSNØKKEL", avstemmingsdata.aksjon.nokkelFom)
        assertEquals("${AVSTEMMINGSNØKKEL + 7}", avstemmingsdata.aksjon.nokkelTom)
        assertNotNull(avstemmingsdata.aksjon.avleverendeAvstemmingId)
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
