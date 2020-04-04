package no.nav.helse.spenn.utbetaling

import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.DetaljType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Fortegn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

internal class OppdragDtoTest {
    private companion object {
        private const val PERSON = "12345678911"
        private const val ORGNR = "123456789"
        private const val UTBETALINGSREF = "a1b0c2"
        private const val AVSTEMMINGSNØKKEL = 1L
        private const val SAKSBEHANDLER = "Navn Navnesen"
        private const val BELØP = 1000
        private val OPPRETTET = LocalDateTime.now()

        private const val AKSEPTERT_UTEN_FEIL = "00"
        private const val AKSEPTERT_MED_FEIL = "04"
        private const val AVVIST_FUNKSJONELLE_FEIL = "08"
        private const val AVVIST_TEKNISK_FEIL = "12"
        private const val UGYLDIG_FEILKODE = "??"
    }

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

    @Test
    fun periode() {
        OppdragDto.periode(oppdrag).also {
            assertEquals(OPPRETTET, it.start)
            assertEquals(OPPRETTET.plusDays(6), it.endInclusive)
        }
        assertThrows<IllegalStateException> { OppdragDto.periode(emptyList()) }
    }

    @Test
    fun avstemmingsperiode() {
        OppdragDto.avstemmingsperiode(oppdrag).also {
            assertEquals(AVSTEMMINGSNØKKEL, it.start)
            assertEquals(AVSTEMMINGSNØKKEL + 7, it.endInclusive)
        }
        assertThrows<IllegalStateException> { OppdragDto.avstemmingsperiode(emptyList()) }
    }

    @Test
    fun totaldata() {
        OppdragDto.totaldata(oppdrag).also {
            assertEquals(oppdrag.size, it.totalAntall)
            assertEquals((4* BELØP).toBigDecimal(), it.totalBelop)
            assertEquals(Fortegn.T, it.fortegn)
        }
    }

    @Test
    fun `totaldata negativt beløp`() {
        OppdragDto.totaldata(listOf(
            OppdragDto(
                AVSTEMMINGSNØKKEL,
                PERSON,
                UTBETALINGSREF,
                OPPRETTET,
                Oppdragstatus.OVERFØRT,
                -1 * BELØP,
                null
            )
        )).also {
            assertEquals(1, it.totalAntall)
            assertEquals((-BELØP).toBigDecimal(), it.totalBelop)
            assertEquals(Fortegn.F, it.fortegn)
        }
    }

    @Test
    fun grunnlagsdata() {
        OppdragDto.grunnlagsdata(oppdrag).also {
            it.godkjentAntall = 2
            it.godkjentBelop = BELØP.toBigDecimal()
            it.godkjentFortegn = Fortegn.T
            it.varselAntall = 1
            it.varselBelop = BELØP.toBigDecimal()
            it.varselFortegn = Fortegn.T
            it.avvistAntall = 3
            it.avvistBelop = (-BELØP).toBigDecimal()
            it.avvistFortegn = Fortegn.F
            it.manglerAntall = 2
            it.manglerBelop = (2* BELØP).toBigDecimal()
            it.manglerFortegn = Fortegn.T
        }
    }

    @Test
    fun detaljer() {
        OppdragDto.detaljer(oppdrag).also {
            assertEquals(6, it.size)
            assertEquals(DetaljType.MANG, it[0].detaljType)
            assertNull(it[0].alvorlighetsgrad)
            assertEquals(DetaljType.VARS, it[1].detaljType)
            assertEquals(AKSEPTERT_MED_FEIL, it[1].alvorlighetsgrad)
            assertEquals(DetaljType.AVVI, it[2].detaljType)
            assertEquals(AVVIST_FUNKSJONELLE_FEIL, it[2].alvorlighetsgrad)
            assertEquals(DetaljType.AVVI, it[3].detaljType)
            assertEquals(DetaljType.AVVI, it[4].detaljType)
            assertEquals(AVVIST_TEKNISK_FEIL, it[4].alvorlighetsgrad)
            assertEquals(DetaljType.MANG, it[5].detaljType)
        }
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
