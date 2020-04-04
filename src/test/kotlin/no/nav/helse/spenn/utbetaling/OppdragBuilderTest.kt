package no.nav.helse.spenn.utbetaling

import no.nav.helse.spenn.Avstemmingsnøkkel
import no.nav.helse.spenn.EndringsKode
import no.nav.helse.spenn.Utbetalingslinjer
import no.nav.helse.spenn.januar
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import javax.xml.datatype.XMLGregorianCalendar

internal class OppdragBuilderTest {

    private companion object {
        private const val PERSON = "12345678911"
        private const val ORGNR = "123456789"
        private const val UTBETALINGSREF = "a1b0c2"
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
    fun `bygger oppdrag`() {
        val oppdrag = oppdrag(false) {
            refusjonTilArbeidsgiver(1.januar, 14.januar,
                DAGSATS,
                GRAD
            )
            utbetalingTilBruker(15.januar, 31.januar,
                DAGSATS,
                GRAD
            )
        }
        assertOppdrag(oppdrag, EndringsKode.NY)
        assertArbeidsgiverlinje(oppdrag, 0, "1", EndringsKode.NY, 1.januar, 14.januar)
        assertBrukerlinje(oppdrag, 1, "2", EndringsKode.NY, 15.januar, 31.januar)
    }

    @Test
    fun `bygger oppdrag med forlengelse`() {
        val oppdrag = oppdrag(true) {
            refusjonTilArbeidsgiver(1.januar, 14.januar,
                DAGSATS,
                GRAD
            )
            utbetalingTilBruker(15.januar, 31.januar,
                DAGSATS,
                GRAD
            )
        }
        assertOppdrag(oppdrag, EndringsKode.UENDRET)
        assertArbeidsgiverlinje(oppdrag, 0, "1",
            EndringsKode.ENDRING, 1.januar, 14.januar)
        assertBrukerlinje(oppdrag, 1, "2", EndringsKode.ENDRING, 15.januar, 31.januar)
    }

    private fun oppdrag(forlengelse: Boolean, block: Utbetalingslinjer.() -> Unit): Oppdrag {
        val builder = OppdragBuilder(
            SAKSBEHANDLER,
            MAKSDATO,
            AVSTEMMINGSNØKKEL,
            Utbetalingslinjer(
                UTBETALINGSREF,
                ORGNR,
                PERSON,
                forlengelse
            ).apply(block),
            NÅ
        )
        return builder.build()
    }

    private fun assertOppdrag(oppdrag: Oppdrag, endringsKode: EndringsKode) {
        assertEquals(PERSON, oppdrag.oppdrag110.oppdragGjelderId)
        assertEquals(SAKSBEHANDLER, oppdrag.oppdrag110.saksbehId)
        assertEquals(UTBETALINGSREF, oppdrag.oppdrag110.fagsystemId)
        assertEquals(AVSTEMMINGSNØKKEL.toString(), oppdrag.oppdrag110.avstemming115.nokkelAvstemming)
        assertEquals(endringsKode.kode, oppdrag.oppdrag110.kodeEndring)
    }

    private fun assertArbeidsgiverlinje(oppdrag: Oppdrag, index: Int, delytelseId: String, endringsKode: EndringsKode, fom: LocalDate, tom: LocalDate) {
        assertOppdragslinje(oppdrag, index, delytelseId, endringsKode, fom, tom)
        assertEquals(MAKSDATO, oppdrag.oppdrag110.oppdragsLinje150[index].refusjonsinfo156.maksDato.toLocalDate())
        assertEquals("00$ORGNR", oppdrag.oppdrag110.oppdragsLinje150[index].refusjonsinfo156.refunderesId)
    }

    private fun assertBrukerlinje(oppdrag: Oppdrag, index: Int, delytelseId: String, endringsKode: EndringsKode, fom: LocalDate, tom: LocalDate) {
        assertOppdragslinje(oppdrag, index, delytelseId, endringsKode, fom, tom)
        assertEquals(PERSON, oppdrag.oppdrag110.oppdragsLinje150[index].utbetalesTilId)
    }

    private fun assertOppdragslinje(oppdrag: Oppdrag, index: Int, delytelseId: String, endringsKode: EndringsKode, fom: LocalDate, tom: LocalDate) {
        assertEquals(delytelseId, oppdrag.oppdrag110.oppdragsLinje150[index].delytelseId)
        assertEquals(endringsKode.kode, oppdrag.oppdrag110.oppdragsLinje150[index].kodeEndringLinje)
        assertEquals(DAGSATS.toBigDecimal(), oppdrag.oppdrag110.oppdragsLinje150[index].sats)
        assertEquals(GRAD.toBigInteger(), oppdrag.oppdrag110.oppdragsLinje150[index].grad170.first().grad)
        assertEquals(SAKSBEHANDLER, oppdrag.oppdrag110.oppdragsLinje150[index].attestant180.first().attestantId)
        assertEquals(fom, oppdrag.oppdrag110.oppdragsLinje150[index].datoVedtakFom.toLocalDate())
        assertEquals(tom, oppdrag.oppdrag110.oppdragsLinje150[index].datoVedtakTom.toLocalDate())
    }
}
