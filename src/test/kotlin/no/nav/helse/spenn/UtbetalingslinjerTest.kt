package no.nav.helse.spenn

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class UtbetalingslinjerTest {

    private companion object {
        private const val PERSON = "12345678911"
        private const val ORGNR = "123456789"
        private const val UTBETALINGSREF = "a1b0c2"
        private const val DAGSATS = 1000
        private const val GRAD = 100
    }

    private lateinit var utbetalingslinjer: Utbetalingslinjer
    private val inspektør get() = UtbetalingslinjerInspektør(utbetalingslinjer)

    @BeforeEach
    fun setup() {
        utbetalingslinjer = Utbetalingslinjer(UTBETALINGSREF, ORGNR, PERSON, false)
    }

    @Test
    fun `ingen utbetalingslinjer`() {
        assertTrue(utbetalingslinjer.isEmpty())
        assertEquals(0, utbetalingslinjer.totalbeløp())
        assertThrows<IllegalStateException> { utbetalingslinjer.førsteDag() }
        assertThrows<IllegalStateException> { utbetalingslinjer.sisteDag() }
    }

    @Test
    fun `første og siste dag`() {
        utbetalingslinjer.apply {
            refusjonTilArbeidsgiver(1.januar, 14.januar, DAGSATS, GRAD)
            refusjonTilArbeidsgiver(15.januar, 31.januar, DAGSATS, GRAD)
        }
        assertEquals(1.januar, utbetalingslinjer.førsteDag())
        assertEquals(31.januar, utbetalingslinjer.sisteDag())
    }

    @Test
    fun `totalbeløp`() {
        utbetalingslinjer.apply {
            refusjonTilArbeidsgiver(1.januar, 14.januar, DAGSATS, GRAD)
            refusjonTilArbeidsgiver(15.januar, 31.januar, DAGSATS, GRAD)
        }
        assertEquals(DAGSATS + DAGSATS, utbetalingslinjer.totalbeløp())
    }

    @Test
    fun `utbetalingslinjer får sekvensiell id`() {
        utbetalingslinjer.apply {
            refusjonTilArbeidsgiver(1.januar, 14.januar, DAGSATS, GRAD)
            utbetalingTilBruker(15.januar, 31.januar, DAGSATS, GRAD)
        }
        assertEquals(1, inspektør.utbetalingslinjeId(0))
        assertEquals(2, inspektør.utbetalingslinjeId(1))
    }

    private class UtbetalingslinjerInspektør(utbetalingslinjer: Utbetalingslinjer) : UtbetalingslinjerVisitor {

        private val utbetalingslinjeIder = mutableMapOf<Int, Int>()
        private var utbetalingslinjerteller = 0

        init {
            utbetalingslinjer.accept(this)
        }

        override fun visitRefusjonTilArbeidsgiver(
            refusjonTilArbeidsgiver: Utbetalingslinjer.Utbetalingslinje.RefusjonTilArbeidsgiver,
            id: Int,
            organisasjonsnummer: String,
            forlengelse: Boolean,
            fom: LocalDate,
            tom: LocalDate,
            dagsats: Int,
            grad: Int
        ) {
            utbetalingslinjeIder[utbetalingslinjerteller] = id
            utbetalingslinjerteller += 1
        }

        override fun visitUtbetalingTilBruker(
            utbetalingTilBruker: Utbetalingslinjer.Utbetalingslinje.UtbetalingTilBruker,
            id: Int,
            fødselsnummer: String,
            forlengelse: Boolean,
            fom: LocalDate,
            tom: LocalDate,
            dagsats: Int,
            grad: Int
        ) {
            utbetalingslinjeIder[utbetalingslinjerteller] = id
            utbetalingslinjerteller += 1
        }

        fun utbetalingslinjeId(index: Int) = utbetalingslinjeIder.getValue(index)
    }

}
