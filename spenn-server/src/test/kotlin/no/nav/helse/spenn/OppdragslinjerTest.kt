package no.nav.helse.spenn

import org.junit.jupiter.api.Assertions.*
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
}
