package no.nav.helse.spenn.simulering

import no.nav.helse.spenn.Utbetalingslinjer
import no.nav.helse.spenn.januar
import no.nav.system.os.entiteter.typer.simpletypes.KodeStatusLinje
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.Oppdrag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
        private val MAKSDATO = LocalDate.MAX
        private val tidsstempel = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    @Test
    fun `bygger simulering request til arbeidsgiver`() {
        val simuleringRequest = simuleringRequestRefusjon(ENDRINGSKODE_ENDRET) {
            linje(Utbetalingslinjer.Utbetalingslinje(1, ENDRINGSKODE_NY, "SPREFAG-IOP", 1.januar, 14.januar, DAGSATS, GRAD, null, null, null, null))
            linje(Utbetalingslinjer.Utbetalingslinje(2, ENDRINGSKODE_NY, "SPREFAG-IOP", 15.januar, 31.januar, DAGSATS, GRAD, null, null, null, null))
        }
        assertEquals(1.januar.format(tidsstempel), simuleringRequest.request.simuleringsPeriode.datoSimulerFom)
        assertEquals(31.januar.format(tidsstempel), simuleringRequest.request.simuleringsPeriode.datoSimulerTom)
        assertOppdrag(simuleringRequest.request.oppdrag, ENDRINGSKODE_ENDRET)
        assertArbeidsgiverlinje(simuleringRequest.request.oppdrag, 0, "1", ENDRINGSKODE_NY, 1.januar, 14.januar)
        assertArbeidsgiverlinje(simuleringRequest.request.oppdrag, 1, "2", ENDRINGSKODE_NY, 15.januar, 31.januar)
    }


    @Test
    fun `bygger simulering request til bruker`() {
        val simuleringRequest = simuleringRequestBruker(ENDRINGSKODE_ENDRET) {
            linje(Utbetalingslinjer.Utbetalingslinje(1, ENDRINGSKODE_NY, "SP", 1.januar, 14.januar, DAGSATS, GRAD, null, null, null, null))
            linje(Utbetalingslinjer.Utbetalingslinje(2, ENDRINGSKODE_NY, "SP", 15.januar, 31.januar, DAGSATS, GRAD, null, null, null, null))
        }
        assertEquals(1.januar.format(tidsstempel), simuleringRequest.request.simuleringsPeriode.datoSimulerFom)
        assertEquals(31.januar.format(tidsstempel), simuleringRequest.request.simuleringsPeriode.datoSimulerTom)
        assertOppdrag(simuleringRequest.request.oppdrag, ENDRINGSKODE_ENDRET)
        assertBrukerlinje(simuleringRequest.request.oppdrag, 0, "1", ENDRINGSKODE_NY, 1.januar, 14.januar)
        assertBrukerlinje(simuleringRequest.request.oppdrag, 1, "2", ENDRINGSKODE_NY, 15.januar, 31.januar)
    }

    @Test
    fun `arbeidsgiverlinje opphører`() {
        val simuleringRequest = simuleringRequestRefusjon(ENDRINGSKODE_ENDRET) {
            linje(Utbetalingslinjer.Utbetalingslinje(1, ENDRINGSKODE_ENDRET, "SPREFAG-IOP", 1.januar, 31.januar, DAGSATS, GRAD, null, null, 1.januar, "OPPH"))
        }
        assertOppdrag(simuleringRequest.request.oppdrag, ENDRINGSKODE_ENDRET)
        assertArbeidsgiverlinje(simuleringRequest.request.oppdrag, 0, "1", ENDRINGSKODE_ENDRET, 1.januar, 31.januar, 1.januar, KodeStatusLinje.OPPH)
    }

    @Test
    fun `brukerlinje opphører`() {
        val simuleringRequest = simuleringRequestBruker(ENDRINGSKODE_ENDRET) {
            linje(Utbetalingslinjer.Utbetalingslinje(1, "ENDR", "SP", 1.januar, 31.januar,
                DAGSATS,
                GRAD, null, null, 1.januar, "OPPH"))
        }
        assertOppdrag(simuleringRequest.request.oppdrag, ENDRINGSKODE_ENDRET)
        assertBrukerlinje(simuleringRequest.request.oppdrag, 0, "1", "ENDR", 1.januar, 31.januar, 1.januar, KodeStatusLinje.OPPH)
    }

    private fun simuleringRequestRefusjon(endringskode: String, block: Utbetalingslinjer.() -> Unit): SimulerBeregningRequest {
        val builder = SimuleringRequestBuilder(Utbetalingslinjer.RefusjonTilArbeidsgiver(PERSON, ORGNR, FAGSYSTEMID,
            endringskode, SAKSBEHANDLER, MAKSDATO).apply(block))
        return builder.build()
    }

    private fun simuleringRequestBruker(endringskode: String, block: Utbetalingslinjer.() -> Unit): SimulerBeregningRequest {
        val builder = SimuleringRequestBuilder(Utbetalingslinjer.UtbetalingTilBruker(PERSON, PERSON, ORGNR, FAGSYSTEMID,
            endringskode, SAKSBEHANDLER, MAKSDATO).apply(block))
        return builder.build()
    }

    private fun assertOppdrag(oppdrag: Oppdrag, endringskode: String) {
        assertEquals(PERSON, oppdrag.oppdragGjelderId)
        assertEquals(SAKSBEHANDLER, oppdrag.saksbehId)
        assertEquals(FAGSYSTEMID, oppdrag.fagsystemId)
        assertEquals(endringskode, oppdrag.kodeEndring)
    }

    private fun assertArbeidsgiverlinje(oppdrag: Oppdrag, index: Int, delytelseId: String, endringskode: String, fom: LocalDate, tom: LocalDate, datoStatusFom: LocalDate? = null, statuskode: KodeStatusLinje? = null) {
        assertOppdragslinje(oppdrag, index, delytelseId, endringskode, fom, tom, datoStatusFom, statuskode)
        assertEquals(MAKSDATO.format(tidsstempel), oppdrag.oppdragslinje[index].refusjonsInfo.maksDato)
        assertEquals("00$ORGNR", oppdrag.oppdragslinje[index].refusjonsInfo.refunderesId)
    }

    private fun assertBrukerlinje(oppdrag: Oppdrag, index: Int, delytelseId: String, endringskode: String, fom: LocalDate, tom: LocalDate, datoStatusFom: LocalDate? = null, statuskode: KodeStatusLinje? = null) {
        assertOppdragslinje(oppdrag, index, delytelseId, endringskode, fom, tom, datoStatusFom, statuskode)
        assertEquals(PERSON, oppdrag.oppdragslinje[index].utbetalesTilId)
    }

    private fun assertOppdragslinje(oppdrag: Oppdrag, index: Int, delytelseId: String, endringskode: String, fom: LocalDate, tom: LocalDate, datoStatusFom: LocalDate?, statuskode: KodeStatusLinje?) {
        assertEquals(delytelseId, oppdrag.oppdragslinje[index].delytelseId)
        assertEquals(endringskode, oppdrag.oppdragslinje[index].kodeEndringLinje)
        assertEquals(DAGSATS.toBigDecimal(), oppdrag.oppdragslinje[index].sats)
        assertEquals(GRAD.toBigInteger(), oppdrag.oppdragslinje[index].grad.first().grad)
        assertEquals(fom.format(tidsstempel), oppdrag.oppdragslinje[index].datoVedtakFom)
        assertEquals(tom.format(tidsstempel), oppdrag.oppdragslinje[index].datoVedtakTom)
        assertEquals(datoStatusFom?.format(tidsstempel), oppdrag.oppdragslinje[index].datoStatusFom)
        assertEquals(statuskode, oppdrag.oppdragslinje[index].kodeStatusLinje)
    }
}
