package no.nav.helse.spenn

import org.junit.jupiter.api.Assertions.*
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

    @BeforeEach
    fun setup() {
        utbetalingslinjer =
            Utbetalingslinjer.RefusjonTilArbeidsgiver(PERSON, ORGNR, UTBETALINGSREF, "NY", "", LocalDate.MAX)
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
            linje(
                Utbetalingslinjer.Utbetalingslinje(
                    1,
                    "NY",
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
                    "NY",
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
        assertEquals(1.januar, utbetalingslinjer.førsteDag())
        assertEquals(31.januar, utbetalingslinjer.sisteDag())
    }

    @Test
    fun totalbeløp() {
        utbetalingslinjer.apply {
            linje(
                Utbetalingslinjer.Utbetalingslinje(
                    1,
                    "NY",
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
                    "NY",
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
        assertEquals(DAGSATS + DAGSATS, utbetalingslinjer.totalbeløp())
    }
}
