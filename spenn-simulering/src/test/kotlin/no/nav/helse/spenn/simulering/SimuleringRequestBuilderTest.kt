package no.nav.helse.spenn.simulering

import no.nav.helse.spenn.Utbetalingslinjer
import no.nav.helse.spenn.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SimuleringRequestBuilderTest {

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
        private val MAKSDATO = LocalDate.EPOCH
    }

    @Test
    fun `bygger simulering request til arbeidsgiver`() {
        val simuleringRequest = simuleringRequestRefusjon(ENDRINGSKODE_ENDRET) {
            linje(
                Utbetalingslinjer.Utbetalingslinje(
                    1,
                    ENDRINGSKODE_NY,
                    "SPREFAG-IOP",
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
        assertEquals(1.januar, simuleringRequest.simuleringsPeriode.datoSimulerFom)
        assertEquals(31.januar, simuleringRequest.simuleringsPeriode.datoSimulerTom)
        assertOppdrag(simuleringRequest.oppdrag, ENDRINGSKODE_ENDRET)
        assertArbeidsgiverlinje(simuleringRequest.oppdrag, 0, "1", ENDRINGSKODE_NY, 1.januar, 14.januar)
        assertArbeidsgiverlinje(simuleringRequest.oppdrag, 1, "2", ENDRINGSKODE_NY, 15.januar, 31.januar)
    }

    @Test
    fun `maksdato som localdate max`() {
        val simuleringRequest = simuleringRequestRefusjon(ENDRINGSKODE_ENDRET, maksdato = LocalDate.MAX) {
            linje(
                Utbetalingslinjer.Utbetalingslinje(
                    1,
                    ENDRINGSKODE_NY,
                    "SPREFAG-IOP",
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
        }
        assertNull(simuleringRequest.oppdrag.oppdragslinje.single().refusjonsInfo?.maksDato)
    }


    @Test
    fun `bygger simulering request til bruker`() {
        val simuleringRequest = simuleringRequestBruker(ENDRINGSKODE_ENDRET) {
            linje(
                Utbetalingslinjer.Utbetalingslinje(
                    1,
                    ENDRINGSKODE_NY,
                    "SP",
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
        assertEquals(1.januar, simuleringRequest.simuleringsPeriode.datoSimulerFom)
        assertEquals(31.januar, simuleringRequest.simuleringsPeriode.datoSimulerTom)
        assertOppdrag(simuleringRequest.oppdrag, ENDRINGSKODE_ENDRET)
        assertBrukerlinje(simuleringRequest.oppdrag, 0, "1", ENDRINGSKODE_NY, 1.januar, 14.januar)
        assertBrukerlinje(simuleringRequest.oppdrag, 1, "2", ENDRINGSKODE_NY, 15.januar, 31.januar)
    }

    @Test
    fun `arbeidsgiverlinje opphører`() {
        val simuleringRequest = simuleringRequestRefusjon(ENDRINGSKODE_ENDRET) {
            linje(
                Utbetalingslinjer.Utbetalingslinje(
                    1,
                    ENDRINGSKODE_ENDRET,
                    "SPREFAG-IOP",
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
        assertOppdrag(simuleringRequest.oppdrag, ENDRINGSKODE_ENDRET)
        assertArbeidsgiverlinje(
            simuleringRequest.oppdrag,
            0,
            "1",
            ENDRINGSKODE_ENDRET,
            1.januar,
            31.januar,
            1.januar,
            KodeStatusLinje.OPPH
        )
    }

    @Test
    fun `brukerlinje opphører`() {
        val simuleringRequest = simuleringRequestBruker(ENDRINGSKODE_ENDRET) {
            linje(
                Utbetalingslinjer.Utbetalingslinje(
                    1, "ENDR", "SP", 1.januar, 31.januar,
                    DAGSATS,
                    GRAD, null, null, 1.januar, "OPPH", "DAG"
                )
            )
        }
        assertOppdrag(simuleringRequest.oppdrag, ENDRINGSKODE_ENDRET)
        assertBrukerlinje(
            simuleringRequest.oppdrag,
            0,
            "1",
            "ENDR",
            1.januar,
            31.januar,
            1.januar,
            KodeStatusLinje.OPPH
        )
    }

    private fun simuleringRequestRefusjon(
        endringskode: String,
        maksdato: LocalDate = MAKSDATO,
        block: Utbetalingslinjer.() -> Unit
    ): SimulerBeregningRequest {
        val builder = SimuleringRequestBuilder(
            Utbetalingslinjer.RefusjonTilArbeidsgiver(
                PERSON, ORGNR, FAGSYSTEMID,
                endringskode, SAKSBEHANDLER, maksdato
            ).apply(block)
        )
        return builder.build()
    }

    private fun simuleringRequestBruker(
        endringskode: String,
        block: Utbetalingslinjer.() -> Unit
    ): SimulerBeregningRequest {
        val builder = SimuleringRequestBuilder(
            Utbetalingslinjer.UtbetalingTilBruker(
                PERSON, PERSON, ORGNR, FAGSYSTEMID,
                endringskode, SAKSBEHANDLER, MAKSDATO
            ).apply(block)
        )
        return builder.build()
    }

    private fun assertOppdrag(oppdrag: Oppdrag, endringskode: String) {
        assertEquals(PERSON, oppdrag.oppdragGjelderId)
        assertEquals(SAKSBEHANDLER, oppdrag.saksbehId)
        assertEquals(FAGSYSTEMID, oppdrag.fagsystemId)
        assertEquals(endringskode, oppdrag.kodeEndring)
    }

    private fun assertArbeidsgiverlinje(
        oppdrag: Oppdrag,
        index: Int,
        delytelseId: String,
        endringskode: String,
        fom: LocalDate,
        tom: LocalDate,
        datoStatusFom: LocalDate? = null,
        statuskode: KodeStatusLinje? = null
    ) {
        assertOppdragslinje(oppdrag, index, delytelseId, endringskode, fom, tom, datoStatusFom, statuskode)
        assertEquals(MAKSDATO, oppdrag.oppdragslinje[index].refusjonsInfo?.maksDato)
        assertEquals("00$ORGNR", oppdrag.oppdragslinje[index].refusjonsInfo?.refunderesId)
    }

    private fun assertBrukerlinje(
        oppdrag: Oppdrag,
        index: Int,
        delytelseId: String,
        endringskode: String,
        fom: LocalDate,
        tom: LocalDate,
        datoStatusFom: LocalDate? = null,
        statuskode: KodeStatusLinje? = null
    ) {
        assertOppdragslinje(oppdrag, index, delytelseId, endringskode, fom, tom, datoStatusFom, statuskode)
        assertEquals(PERSON, oppdrag.oppdragslinje[index].utbetalesTilId)
    }

    private fun assertOppdragslinje(
        oppdrag: Oppdrag,
        index: Int,
        delytelseId: String,
        endringskode: String,
        fom: LocalDate,
        tom: LocalDate,
        datoStatusFom: LocalDate?,
        statuskode: KodeStatusLinje?
    ) {
        assertEquals(delytelseId, oppdrag.oppdragslinje[index].delytelseId)
        assertEquals(endringskode, oppdrag.oppdragslinje[index].kodeEndringLinje)
        assertEquals(DAGSATS, oppdrag.oppdragslinje[index].sats)
        assertEquals(GRAD, oppdrag.oppdragslinje[index].grad.first().grad)
        assertEquals(fom, oppdrag.oppdragslinje[index].datoVedtakFom)
        assertEquals(tom, oppdrag.oppdragslinje[index].datoVedtakTom)
        assertEquals(datoStatusFom, oppdrag.oppdragslinje[index].datoStatusFom)
        assertEquals(statuskode, oppdrag.oppdragslinje[index].kodeStatusLinje)
    }
}
