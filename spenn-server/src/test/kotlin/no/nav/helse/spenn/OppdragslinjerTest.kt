package no.nav.helse.spenn

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class OppdragslinjerTest {

    private companion object {
        private const val PERSON = "12345678911"
        private const val ORGNR = "123456789"
        private const val UTBETALINGSREF = "a1b0c2"
        private const val DAGSATS = 1000
        private const val GRAD = 100
    }

    private lateinit var oppdragslinjer: Oppdragslinjer
    private val inspektør get() = OppdragslinjerInspektør(oppdragslinjer)

    @BeforeEach
    fun setup() {
        oppdragslinjer = Oppdragslinjer(UTBETALINGSREF, ORGNR, PERSON, false, LocalDate.MAX)
    }

    @Test
    fun `ingen oppdragslinjer`() {
        assertTrue(oppdragslinjer.isEmpty())
        assertThrows<IllegalStateException> { oppdragslinjer.førsteDag() }
        assertThrows<IllegalStateException> { oppdragslinjer.sisteDag() }
    }

    @Test
    fun `første og siste dag`() {
        oppdragslinjer.apply {
            refusjonTilArbeidsgiver(1.januar, 14.januar, DAGSATS, GRAD)
            refusjonTilArbeidsgiver(15.januar, 31.januar, DAGSATS, GRAD)
        }
        assertEquals(1.januar, oppdragslinjer.førsteDag())
        assertEquals(31.januar, oppdragslinjer.sisteDag())
    }

    @Test
    fun `oppdragslinje får sekvensiell id`() {
        oppdragslinjer.apply {
            refusjonTilArbeidsgiver(1.januar, 14.januar, DAGSATS, GRAD)
            utbetalingTilBruker(15.januar, 31.januar, DAGSATS, GRAD)
        }
        assertEquals(1, inspektør.oppdragslinjeId(0))
        assertEquals(2, inspektør.oppdragslinjeId(1))
    }

    private class OppdragslinjerInspektør(oppdragslinjer: Oppdragslinjer) : OppdragslinjerVisitor {

        private val oppdragslinjeIdMap = mutableMapOf<Int, Int>()
        private var oppdragslinjeteller = 0

        init {
            oppdragslinjer.accept(this)
        }

        override fun visitRefusjonTilArbeidsgiver(refusjonTilArbeidsgiver: Oppdragslinjer.Utbetalingslinje.RefusjonTilArbeidsgiver) {
            oppdragslinjeIdMap[oppdragslinjeteller] = refusjonTilArbeidsgiver.id
            oppdragslinjeteller += 1
        }

        override fun visitUtbetalingTilBruker(utbetalingTilBruker: Oppdragslinjer.Utbetalingslinje.UtbetalingTilBruker) {
            oppdragslinjeIdMap[oppdragslinjeteller] = utbetalingTilBruker.id
            oppdragslinjeteller += 1
        }

        fun oppdragslinjeId(index: Int) = oppdragslinjeIdMap.getValue(index)
    }

}
