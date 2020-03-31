package no.nav.helse.spenn

import no.nav.helse.spenn.oppdrag.EndringsKode
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.Oppdrag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal class OppdragSimuleringRequestBuilderTest {

    private companion object {
        private const val PERSON = "12345678911"
        private const val ORGNR = "123456789"
        private const val UTBETALINGSREF = "a1b0c2"
        private const val DAGSATS = 1000
        private const val GRAD = 100
        private const val SAKSBEHANDLER = "Spenn"
        private val tidsstempel = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    @Test
    fun `bygger simulering request`() {
        val oppdrag = oppdrag(false){
            refusjonTilArbeidsgiver(1.januar, 14.januar, DAGSATS, GRAD)
            utbetalingTilBruker(15.januar, 31.januar, DAGSATS, GRAD)
        }
        assertOppdrag(oppdrag, EndringsKode.NY)
        assertArbeidsgiverlinje(oppdrag, 0, "1", EndringsKode.NY, 1.januar, 14.januar)
        assertBrukerlinje(oppdrag, 1, "2", EndringsKode.NY, 15.januar, 31.januar)
    }

    @Test
    fun `bygger simulering request med forlengelse`() {
        val oppdrag = oppdrag(true) {
            refusjonTilArbeidsgiver(1.januar, 14.januar, DAGSATS, GRAD)
            utbetalingTilBruker(15.januar, 31.januar, DAGSATS, GRAD)
        }
        assertOppdrag(oppdrag, EndringsKode.UENDRET)
        assertArbeidsgiverlinje(oppdrag, 0, "1", EndringsKode.ENDRING, 1.januar, 14.januar)
        assertBrukerlinje(oppdrag, 1, "2", EndringsKode.ENDRING, 15.januar, 31.januar)
    }

    private fun oppdrag(forlengelse: Boolean, block: Oppdragslinjer.() -> Unit): Oppdrag {
        val builder = OppdragSimuleringRequestBuilder(
            SAKSBEHANDLER,
            Oppdragslinjer(UTBETALINGSREF, ORGNR, PERSON, forlengelse, LocalDate.MAX).apply(block))
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
        assertEquals("00$ORGNR", oppdrag.oppdragslinje[index].refusjonsInfo.refunderesId)
    }

    private fun assertBrukerlinje(oppdrag: Oppdrag, index: Int, delytelseId: String, endringsKode: EndringsKode, fom: LocalDate, tom: LocalDate) {
        assertOppdragslinje(oppdrag, index, delytelseId, endringsKode, fom, tom)
        assertEquals(PERSON, oppdrag.oppdragslinje[index].utbetalesTilId)
    }

    private fun assertOppdragslinje(oppdrag: Oppdrag, index: Int, delytelseId: String, endringsKode: EndringsKode, fom: LocalDate, tom: LocalDate) {
        assertEquals(delytelseId, oppdrag.oppdragslinje[index].delytelseId)
        assertEquals(endringsKode.kode, oppdrag.oppdragslinje[index].kodeEndringLinje)
        assertEquals(fom.format(tidsstempel), oppdrag.oppdragslinje[index].datoVedtakFom)
        assertEquals(tom.format(tidsstempel), oppdrag.oppdragslinje[index].datoVedtakTom)
    }
}
