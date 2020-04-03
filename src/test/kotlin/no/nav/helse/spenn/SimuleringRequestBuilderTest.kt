package no.nav.helse.spenn

import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.Oppdrag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal class SimuleringRequestBuilderTest {

    private companion object {
        private const val PERSON = "12345678911"
        private const val ORGNR = "123456789"
        private const val UTBETALINGSREF = "a1b0c2"
        private const val DAGSATS = 1000
        private const val GRAD = 100
        private const val SAKSBEHANDLER = "Spenn"
        private val MAKSDATO = LocalDate.MAX
        private val tidsstempel = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    @Test
    fun `bygger simulering request`() {
        val simuleringRequest = simuleringRequest(false) {
            refusjonTilArbeidsgiver(1.januar, 14.januar, DAGSATS, GRAD)
            utbetalingTilBruker(15.januar, 31.januar, DAGSATS, GRAD)
        }
        assertEquals(1.januar.format(tidsstempel), simuleringRequest.request.simuleringsPeriode.datoSimulerFom)
        assertEquals(31.januar.format(tidsstempel), simuleringRequest.request.simuleringsPeriode.datoSimulerTom)
        assertOppdrag(simuleringRequest.request.oppdrag, EndringsKode.NY)
        assertArbeidsgiverlinje(simuleringRequest.request.oppdrag, 0, "1", EndringsKode.NY, 1.januar, 14.januar)
        assertBrukerlinje(simuleringRequest.request.oppdrag, 1, "2", EndringsKode.NY, 15.januar, 31.januar)
    }

    @Test
    fun `bygger simulering request med forlengelse`() {
        val simuleringRequest = simuleringRequest(true) {
            refusjonTilArbeidsgiver(1.januar, 14.januar, DAGSATS, GRAD)
            utbetalingTilBruker(15.januar, 31.januar, DAGSATS, GRAD)
        }
        assertEquals(1.januar.format(tidsstempel), simuleringRequest.request.simuleringsPeriode.datoSimulerFom)
        assertEquals(31.januar.format(tidsstempel), simuleringRequest.request.simuleringsPeriode.datoSimulerTom)
        assertOppdrag(simuleringRequest.request.oppdrag, EndringsKode.UENDRET)
        assertArbeidsgiverlinje(simuleringRequest.request.oppdrag, 0, "1", EndringsKode.ENDRING, 1.januar, 14.januar)
        assertBrukerlinje(simuleringRequest.request.oppdrag, 1, "2", EndringsKode.ENDRING, 15.januar, 31.januar)
    }

    private fun simuleringRequest(forlengelse: Boolean, block: Utbetalingslinjer.() -> Unit): SimulerBeregningRequest {
        val builder = SimuleringRequestBuilder(
            SAKSBEHANDLER,
            MAKSDATO,
            Utbetalingslinjer(UTBETALINGSREF, ORGNR, PERSON, forlengelse).apply(block))
        return builder.build()
    }

    private fun assertOppdrag(oppdrag: Oppdrag, endringsKode: EndringsKode) {
        assertEquals(PERSON, oppdrag.oppdragGjelderId)
        assertEquals(SAKSBEHANDLER, oppdrag.saksbehId)
        assertEquals(UTBETALINGSREF, oppdrag.fagsystemId)
        assertEquals(endringsKode.kode, oppdrag.kodeEndring)
    }

    private fun assertArbeidsgiverlinje(oppdrag: Oppdrag, index: Int, delytelseId: String, endringsKode: EndringsKode, fom: LocalDate, tom: LocalDate) {
        assertOppdragslinje(oppdrag, index, delytelseId, endringsKode, fom, tom)
        assertEquals(MAKSDATO.format(tidsstempel), oppdrag.oppdragslinje[index].refusjonsInfo.maksDato)
        assertEquals("00$ORGNR", oppdrag.oppdragslinje[index].refusjonsInfo.refunderesId)
    }

    private fun assertBrukerlinje(oppdrag: Oppdrag, index: Int, delytelseId: String, endringsKode: EndringsKode, fom: LocalDate, tom: LocalDate) {
        assertOppdragslinje(oppdrag, index, delytelseId, endringsKode, fom, tom)
        assertEquals(PERSON, oppdrag.oppdragslinje[index].utbetalesTilId)
    }

    private fun assertOppdragslinje(oppdrag: Oppdrag, index: Int, delytelseId: String, endringsKode: EndringsKode, fom: LocalDate, tom: LocalDate) {
        assertEquals(delytelseId, oppdrag.oppdragslinje[index].delytelseId)
        assertEquals(endringsKode.kode, oppdrag.oppdragslinje[index].kodeEndringLinje)
        assertEquals(DAGSATS.toBigDecimal(), oppdrag.oppdragslinje[index].sats)
        assertEquals(GRAD.toBigInteger(), oppdrag.oppdragslinje[index].grad.first().grad)
        assertEquals(fom.format(tidsstempel), oppdrag.oppdragslinje[index].datoVedtakFom)
        assertEquals(tom.format(tidsstempel), oppdrag.oppdragslinje[index].datoVedtakTom)
    }
}
