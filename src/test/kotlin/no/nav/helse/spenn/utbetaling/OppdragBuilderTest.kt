package no.nav.helse.spenn.utbetaling

import no.nav.helse.spenn.Avstemmingsnøkkel
import no.nav.helse.spenn.Utbetalingslinjer
import no.nav.helse.spenn.januar
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import no.trygdeetaten.skjema.oppdrag.TkodeStatusLinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import javax.xml.datatype.XMLGregorianCalendar

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
        private val NÅ = Instant.now()
        private val MAKSDATO = LocalDate.now()
        private val AVSTEMMINGSNØKKEL =
            Avstemmingsnøkkel.opprett(NÅ)

        private fun XMLGregorianCalendar.toLocalDate() = toGregorianCalendar()
            .toZonedDateTime()
            .toLocalDate()
    }

    @Test
    fun `bygger oppdrag til arbeidsgiver`() {
        val oppdrag = oppdragRefusjon(ENDRINGSKODE_ENDRET) {
            linje(Utbetalingslinjer.Utbetalingslinje(1, ENDRINGSKODE_NY, "SPREFAG-IOP", 1.januar, 14.januar, DAGSATS, GRAD, null, null, null, null))
            linje(Utbetalingslinjer.Utbetalingslinje(2, ENDRINGSKODE_NY, "SPREFAG-IOP", 15.januar, 31.januar, DAGSATS, GRAD, null, null, null, null))
        }
        assertOppdrag(oppdrag, ENDRINGSKODE_ENDRET)
        assertArbeidsgiverlinje(oppdrag, 0, "1", ENDRINGSKODE_NY, 1.januar, 14.januar)
        assertArbeidsgiverlinje(oppdrag, 1, "2", ENDRINGSKODE_NY, 15.januar, 31.januar)
    }

    @Test
    fun `bygger oppdrag til bruker`() {
        val oppdrag = oppdragBruker(ENDRINGSKODE_UENDRET) {
            linje(Utbetalingslinjer.Utbetalingslinje(1, ENDRINGSKODE_NY, "SP", 1.januar, 14.januar, DAGSATS, GRAD, null, null, null, null))
            linje(Utbetalingslinjer.Utbetalingslinje(2, ENDRINGSKODE_NY, "SP", 15.januar, 31.januar, DAGSATS, GRAD, null, null, null, null))
        }
        assertOppdrag(oppdrag, ENDRINGSKODE_UENDRET)
        assertBrukerlinje(oppdrag, 0, "1", ENDRINGSKODE_NY, 1.januar, 14.januar)
        assertBrukerlinje(oppdrag, 1, "2", ENDRINGSKODE_NY, 15.januar, 31.januar)
    }

    @Test
    fun `arbeidsgiverlinje opphører`() {
        val oppdrag = oppdragRefusjon(ENDRINGSKODE_ENDRET) {
            linje(Utbetalingslinjer.Utbetalingslinje(1, ENDRINGSKODE_ENDRET, "SPREFAG-IOP", 1.januar, 31.januar, DAGSATS, GRAD, null, null, 1.januar, "OPPH"))
        }
        assertOppdrag(oppdrag, ENDRINGSKODE_ENDRET)
        assertArbeidsgiverlinje(oppdrag, 0, "1", ENDRINGSKODE_ENDRET, 1.januar, 31.januar, 1.januar, TkodeStatusLinje.OPPH)
    }

    @Test
    fun `brukerlinje opphører`() {
        val oppdrag = oppdragBruker(ENDRINGSKODE_ENDRET) {
            linje(Utbetalingslinjer.Utbetalingslinje(1, ENDRINGSKODE_ENDRET, "SP", 1.januar, 31.januar, DAGSATS, GRAD, null, null, 1.januar, "OPPH"))
        }
        assertOppdrag(oppdrag, ENDRINGSKODE_ENDRET)
        assertBrukerlinje(oppdrag, 0, "1", ENDRINGSKODE_ENDRET, 1.januar, 31.januar, 1.januar, TkodeStatusLinje.OPPH)
    }


    private fun oppdragRefusjon(endringskode: String, block: Utbetalingslinjer.() -> Unit): Oppdrag {
        val builder = OppdragBuilder(
            Utbetalingslinjer.RefusjonTilArbeidsgiver(PERSON, ORGNR, FAGSYSTEMID, endringskode,
                SAKSBEHANDLER, MAKSDATO).apply(block),
            AVSTEMMINGSNØKKEL,
            NÅ
        )
        return builder.build()
    }

    private fun oppdragBruker(endringskode: String, block: Utbetalingslinjer.() -> Unit): Oppdrag {
        val builder = OppdragBuilder(
            Utbetalingslinjer.UtbetalingTilBruker(PERSON, PERSON, ORGNR, FAGSYSTEMID, endringskode,
                SAKSBEHANDLER, MAKSDATO).apply(block),
            AVSTEMMINGSNØKKEL,
            NÅ
        )
        return builder.build()
    }

    private fun assertOppdrag(oppdrag: Oppdrag, endringskode: String) {
        assertEquals(PERSON, oppdrag.oppdrag110.oppdragGjelderId)
        assertEquals(SAKSBEHANDLER, oppdrag.oppdrag110.saksbehId)
        assertEquals(FAGSYSTEMID, oppdrag.oppdrag110.fagsystemId)
        assertEquals(AVSTEMMINGSNØKKEL.toString(), oppdrag.oppdrag110.avstemming115.nokkelAvstemming)
        assertEquals(endringskode, oppdrag.oppdrag110.kodeEndring)
    }

    private fun assertArbeidsgiverlinje(oppdrag: Oppdrag, index: Int, delytelseId: String, endringskode: String, fom: LocalDate, tom: LocalDate, datoStatusFom: LocalDate? = null, statuskode: TkodeStatusLinje? = null) {
        assertOppdragslinje(oppdrag, index, delytelseId, endringskode, fom, tom, datoStatusFom, statuskode)
        assertEquals(MAKSDATO, oppdrag.oppdrag110.oppdragsLinje150[index].refusjonsinfo156.maksDato.toLocalDate())
        assertEquals("00$ORGNR", oppdrag.oppdrag110.oppdragsLinje150[index].refusjonsinfo156.refunderesId)
    }

    private fun assertBrukerlinje(oppdrag: Oppdrag, index: Int, delytelseId: String, endringskode: String, fom: LocalDate, tom: LocalDate, datoStatusFom: LocalDate? = null, statuskode: TkodeStatusLinje? = null) {
        assertOppdragslinje(oppdrag, index, delytelseId, endringskode, fom, tom, datoStatusFom, statuskode)
        assertEquals(PERSON, oppdrag.oppdrag110.oppdragsLinje150[index].utbetalesTilId)
    }

    private fun assertOppdragslinje(oppdrag: Oppdrag, index: Int, delytelseId: String, endringskode: String, fom: LocalDate, tom: LocalDate, datoStatusFom: LocalDate?, statuskode: TkodeStatusLinje?) {
        assertEquals(delytelseId, oppdrag.oppdrag110.oppdragsLinje150[index].delytelseId)
        assertEquals(endringskode, oppdrag.oppdrag110.oppdragsLinje150[index].kodeEndringLinje)
        assertEquals(DAGSATS.toBigDecimal(), oppdrag.oppdrag110.oppdragsLinje150[index].sats)
        assertEquals(GRAD.toBigInteger(), oppdrag.oppdrag110.oppdragsLinje150[index].grad170.first().grad)
        assertEquals(SAKSBEHANDLER, oppdrag.oppdrag110.oppdragsLinje150[index].attestant180.first().attestantId)
        assertEquals(fom, oppdrag.oppdrag110.oppdragsLinje150[index].datoVedtakFom.toLocalDate())
        assertEquals(tom, oppdrag.oppdrag110.oppdragsLinje150[index].datoVedtakTom.toLocalDate())
        assertEquals(datoStatusFom, oppdrag.oppdrag110.oppdragsLinje150[index].datoStatusFom?.toLocalDate())
        assertEquals(statuskode, oppdrag.oppdrag110.oppdragsLinje150[index].kodeStatusLinje)
    }
}
