package no.nav.helse.spenn.oppdrag

import no.nav.helse.spenn.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class OppdragBuilderTest {

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
        private val NÅ = LocalDateTime.now()
        private val MAKSDATO = LocalDate.now()
        private val AVSTEMMINGSNØKKEL = 1024L
        private val UTBETALING_ID = UUID.randomUUID()
    }

    @Test
    fun `bygger oppdrag til arbeidsgiver`() {
        val oppdrag = oppdragRefusjon(ENDRINGSKODE_ENDRET) {
            linje(
                Utbetalingslinjer.Utbetalingslinje(
                    1,
                    ENDRINGSKODE_NY,
                    "SPREFAG-IOP",
                    1.januar,
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
        assertOppdrag(oppdrag, ENDRINGSKODE_ENDRET)
        assertArbeidsgiverlinje(oppdrag, 0, 1, ENDRINGSKODE_NY, 1.januar, 14.januar)
        assertArbeidsgiverlinje(oppdrag, 1, 2, ENDRINGSKODE_NY, 15.januar, 31.januar)
    }

    @Test
    fun `bygger oppdrag til bruker`() {
        val oppdrag = oppdragBruker(ENDRINGSKODE_UENDRET) {
            linje(
                Utbetalingslinjer.Utbetalingslinje(
                    1,
                    ENDRINGSKODE_NY,
                    "SP",
                    1.januar,
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
                    "SP",
                    15.januar,
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
        assertOppdrag(oppdrag, ENDRINGSKODE_UENDRET)
        assertBrukerlinje(oppdrag, 0, 1, ENDRINGSKODE_NY, 1.januar, 14.januar)
        assertBrukerlinje(oppdrag, 1, 2, ENDRINGSKODE_NY, 15.januar, 31.januar)
    }

    @Test
    fun `arbeidsgiverlinje opphører`() {
        val oppdrag = oppdragRefusjon(ENDRINGSKODE_ENDRET) {
            linje(
                Utbetalingslinjer.Utbetalingslinje(
                    1,
                    ENDRINGSKODE_ENDRET,
                    "SPREFAG-IOP",
                    1.januar,
                    1.januar,
                    31.januar,
                    DAGSATS,
                    GRAD,
                    null,
                    null,
                    1.januar,
                    "OPPH",
                    "DAG"
                )
            )
        }
        assertOppdrag(oppdrag, ENDRINGSKODE_ENDRET)
        assertArbeidsgiverlinje(
            oppdrag,
            0,
            1,
            ENDRINGSKODE_ENDRET,
            1.januar,
            31.januar,
            1.januar,
            StatuskodeLinjeDto.OPPH
        )
    }

    @Test
    fun `brukerlinje opphører`() {
        val oppdrag = oppdragBruker(ENDRINGSKODE_ENDRET) {
            linje(
                Utbetalingslinjer.Utbetalingslinje(
                    1,
                    ENDRINGSKODE_ENDRET,
                    "SP",
                    1.januar,
                    1.januar,
                    31.januar,
                    DAGSATS,
                    GRAD,
                    null,
                    null,
                    1.januar,
                    "OPPH",
                    "DAG"
                )
            )
        }
        assertOppdrag(oppdrag, ENDRINGSKODE_ENDRET)
        assertBrukerlinje(oppdrag, 0, 1, ENDRINGSKODE_ENDRET, 1.januar, 31.januar, 1.januar, StatuskodeLinjeDto.OPPH)
    }


    private fun oppdragRefusjon(endringskode: String, block: Utbetalingslinjer.() -> Unit): OppdragDto {
        val builder = OppdragBuilder(
            UTBETALING_ID,
            Utbetalingslinjer.RefusjonTilArbeidsgiver(PERSON, ORGNR, FAGSYSTEMID, endringskode, SAKSBEHANDLER, MAKSDATO).apply(block),
            AVSTEMMINGSNØKKEL,
            NÅ
        )
        return builder.build()
    }

    private fun oppdragBruker(endringskode: String, block: Utbetalingslinjer.() -> Unit): OppdragDto {
        val builder = OppdragBuilder(
            UTBETALING_ID,
            Utbetalingslinjer.UtbetalingTilBruker(PERSON, PERSON, FAGSYSTEMID, endringskode, SAKSBEHANDLER, MAKSDATO).apply(block),
            AVSTEMMINGSNØKKEL,
            NÅ
        )
        return builder.build()
    }

    private fun assertOppdrag(oppdrag: OppdragDto, endringskode: String) {
        assertEquals(PERSON, oppdrag.oppdrag110.oppdragGjelderId)
        assertEquals(SAKSBEHANDLER, oppdrag.oppdrag110.saksbehId)
        assertEquals(FAGSYSTEMID, oppdrag.oppdrag110.fagsystemId)
        assertEquals(AVSTEMMINGSNØKKEL, oppdrag.oppdrag110.avstemming115.nokkelAvstemming)
        assertEquals(endringskode, oppdrag.oppdrag110.kodeEndring.name)
    }

    private fun assertArbeidsgiverlinje(
        oppdrag: OppdragDto,
        index: Int,
        delytelseId: Int,
        endringskode: String,
        fom: LocalDate,
        tom: LocalDate,
        datoStatusFom: LocalDate? = null,
        statuskode: StatuskodeLinjeDto? = null
    ) {
        assertOppdragslinje(oppdrag, index, delytelseId, endringskode, fom, tom, datoStatusFom, statuskode)
        assertEquals(MAKSDATO, oppdrag.oppdrag110.oppdragsLinje150[index].refusjonsinfo156?.maksDato)
        assertEquals("00$ORGNR", oppdrag.oppdrag110.oppdragsLinje150[index].refusjonsinfo156?.refunderesId)
    }

    private fun assertBrukerlinje(
        oppdrag: OppdragDto,
        index: Int,
        delytelseId: Int,
        endringskode: String,
        fom: LocalDate,
        tom: LocalDate,
        datoStatusFom: LocalDate? = null,
        statuskode: StatuskodeLinjeDto? = null
    ) {
        assertOppdragslinje(oppdrag, index, delytelseId, endringskode, fom, tom, datoStatusFom, statuskode)
        assertEquals(PERSON, oppdrag.oppdrag110.oppdragsLinje150[index].utbetalesTilId)
    }

    private fun assertOppdragslinje(
        oppdrag: OppdragDto,
        index: Int,
        delytelseId: Int,
        endringskode: String,
        fom: LocalDate,
        tom: LocalDate,
        datoStatusFom: LocalDate?,
        statuskode: StatuskodeLinjeDto?
    ) {
        assertEquals(delytelseId, oppdrag.oppdrag110.oppdragsLinje150[index].delytelseId)
        assertEquals(endringskode, oppdrag.oppdrag110.oppdragsLinje150[index].kodeEndringLinje.name)
        assertEquals(DAGSATS, oppdrag.oppdrag110.oppdragsLinje150[index].sats)
        assertEquals(GRAD, oppdrag.oppdrag110.oppdragsLinje150[index].grad170.first().grad)
        assertEquals(SAKSBEHANDLER, oppdrag.oppdrag110.oppdragsLinje150[index].attestant180.first().attestantId)
        assertEquals(fom, oppdrag.oppdrag110.oppdragsLinje150[index].datoVedtakFom)
        assertEquals(tom, oppdrag.oppdrag110.oppdragsLinje150[index].datoVedtakTom)
        assertEquals(datoStatusFom, oppdrag.oppdrag110.oppdragsLinje150[index].datoStatusFom)
        assertEquals(statuskode, oppdrag.oppdrag110.oppdragsLinje150[index].kodeStatusLinje)
    }
}
