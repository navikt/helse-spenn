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
        utbetalingslinjer = Utbetalingslinjer.RefusjonTilArbeidsgiver(PERSON, ORGNR, UTBETALINGSREF, "NY", "", LocalDate.MAX)
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
            linje(Utbetalingslinjer.Utbetalingslinje(1, "NY", "SPREFAG-IOP", 1.januar, 14.januar, DAGSATS, GRAD, null, null, null, null))
            linje(Utbetalingslinjer.Utbetalingslinje(2, "NY", "SPREFAG-IOP", 15.januar, 31.januar, DAGSATS, GRAD, null, null, null, null))
        }
        assertEquals(1.januar, utbetalingslinjer.førsteDag())
        assertEquals(31.januar, utbetalingslinjer.sisteDag())
    }

    @Test
    fun totalbeløp() {
        utbetalingslinjer.apply {
            linje(Utbetalingslinjer.Utbetalingslinje(1, "NY", "SPREFAG-IOP", 1.januar, 14.januar, DAGSATS, GRAD, null, null, null, null))
            linje(Utbetalingslinjer.Utbetalingslinje(2, "NY", "SPREFAG-IOP", 15.januar, 31.januar, DAGSATS, GRAD, null, null, null, null))
        }
        assertEquals(DAGSATS + DAGSATS, utbetalingslinjer.totalbeløp())
    }

    @Test
    fun `Sjekksum er ulik for to oppdrag når mottaker er forskjellig`() {
        val oppdrag1 = Utbetalingslinjer.RefusjonTilArbeidsgiver(PERSON, ORGNR, UTBETALINGSREF, "NY", "", LocalDate.MAX).apply {
            linje(Utbetalingslinjer.Utbetalingslinje(1, "NY", "SPREFAG-IOP", 1.januar, 14.januar, DAGSATS, GRAD, null, null, null, null))
        }
        val oppdrag2 = Utbetalingslinjer.RefusjonTilArbeidsgiver(PERSON, "Et annet orgnr", UTBETALINGSREF, "NY", "", LocalDate.MAX).apply {
            linje(Utbetalingslinjer.Utbetalingslinje(1, "NY", "SPREFAG-IOP", 1.januar, 14.januar, DAGSATS, GRAD, null, null, null, null))
        }

        assertNotEquals(oppdrag1.hashCode(), oppdrag2.hashCode())
    }

    @Test
    fun `Sjekksum er ulik for to oppdrag når person er forskjellig`() {
        val oppdrag1 = Utbetalingslinjer.RefusjonTilArbeidsgiver(PERSON, ORGNR, UTBETALINGSREF, "NY", "", LocalDate.MAX).apply {
            linje(Utbetalingslinjer.Utbetalingslinje(1, "NY", "SPREFAG-IOP", 1.januar, 14.januar, DAGSATS, GRAD, null, null, null, null))
        }
        val oppdrag2 = Utbetalingslinjer.RefusjonTilArbeidsgiver("en annen person", ORGNR, UTBETALINGSREF, "NY", "", LocalDate.MAX).apply {
            linje(Utbetalingslinjer.Utbetalingslinje(1, "NY", "SPREFAG-IOP", 1.januar, 14.januar, DAGSATS, GRAD, null, null, null, null))
        }

        assertNotEquals(oppdrag1.hashCode(), oppdrag2.hashCode())
    }

    @Test
    fun `Sjekksum er ulik for person-oppdrag og arbeidsgiveroppdrag`() {
        val oppdrag1 = Utbetalingslinjer.RefusjonTilArbeidsgiver(PERSON, ORGNR, UTBETALINGSREF, "NY", "", LocalDate.MAX).apply {
            linje(Utbetalingslinjer.Utbetalingslinje(1, "NY", "SPREFAG-IOP", 1.januar, 14.januar, DAGSATS, GRAD, null, null, null, null))
        }
        val oppdrag2 = Utbetalingslinjer.UtbetalingTilBruker(PERSON, PERSON, "ET ANNET ORGNR", UTBETALINGSREF, "NY", "", LocalDate.MAX).apply {
            linje(Utbetalingslinjer.Utbetalingslinje(1, "NY", "SP", 1.januar, 14.januar, DAGSATS, GRAD, null, null, null, null))
        }

        assertNotEquals(oppdrag1.hashCode(), oppdrag2.hashCode())
    }

    @Test
    fun `Sjekksum er ulik for to person-oppdrag når mottaker er forskjellig`() {
        val oppdrag1 = Utbetalingslinjer.UtbetalingTilBruker(PERSON, PERSON, ORGNR, UTBETALINGSREF, "NY", "", LocalDate.MAX).apply {
            linje(Utbetalingslinjer.Utbetalingslinje(1, "NY", "SP", 1.januar, 14.januar, DAGSATS, GRAD, null, null, null, null))
        }
        val oppdrag2 = Utbetalingslinjer.UtbetalingTilBruker(PERSON, PERSON, "ET ANNET ORGNR", UTBETALINGSREF, "NY", "", LocalDate.MAX).apply {
            linje(Utbetalingslinjer.Utbetalingslinje(1, "NY", "SP", 1.januar, 14.januar, DAGSATS, GRAD, null, null, null, null))
        }

        assertNotEquals(oppdrag1.hashCode(), oppdrag2.hashCode())
    }

    @Test
    fun `Sjekksum er ulik for to oppdrag når fom er forskjellig`() {
        val oppdrag1 = Utbetalingslinjer.RefusjonTilArbeidsgiver(PERSON, ORGNR, UTBETALINGSREF, "NY", "", LocalDate.MAX).apply {
            linje(Utbetalingslinjer.Utbetalingslinje(1, "NY", "SPREFAG-IOP", 1.januar, 14.januar, DAGSATS, GRAD, null, null, null, null))
        }
        val oppdrag2 = Utbetalingslinjer.RefusjonTilArbeidsgiver(PERSON, ORGNR, UTBETALINGSREF, "NY", "", LocalDate.MAX).apply {
            linje(Utbetalingslinjer.Utbetalingslinje(1, "NY", "SPREFAG-IOP", 2.januar, 14.januar, DAGSATS, GRAD, null, null, null, null))
        }

        assertNotEquals(oppdrag1.hashCode(), oppdrag2.hashCode())
    }

    @Test
    fun `Sjekksum er ulik for to oppdrag når tom er forskjellig`() {
        val oppdrag1 = Utbetalingslinjer.RefusjonTilArbeidsgiver(PERSON, ORGNR, UTBETALINGSREF, "NY", "", LocalDate.MAX).apply {
            linje(Utbetalingslinjer.Utbetalingslinje(1, "NY", "SPREFAG-IOP", 1.januar, 14.januar, DAGSATS, GRAD, null, null, null, null))
        }
        val oppdrag2 = Utbetalingslinjer.RefusjonTilArbeidsgiver(PERSON, ORGNR, UTBETALINGSREF, "NY", "", LocalDate.MAX).apply {
            linje(Utbetalingslinjer.Utbetalingslinje(1, "NY", "SPREFAG-IOP", 1.januar, 15.januar, DAGSATS, GRAD, null, null, null, null))
        }

        assertNotEquals(oppdrag1.hashCode(), oppdrag2.hashCode())
    }

    @Test
    fun `Sjekksum er ulik for to oppdrag når dagsats og grad er forskjellig`() {
        val oppdrag1 = Utbetalingslinjer.RefusjonTilArbeidsgiver(PERSON, ORGNR, UTBETALINGSREF, "NY", "", LocalDate.MAX).apply {
            linje(Utbetalingslinjer.Utbetalingslinje(1, "NY", "SPREFAG-IOP", 1.januar, 14.januar, DAGSATS, GRAD, null, null, null, null))
        }
        val oppdrag2 = Utbetalingslinjer.RefusjonTilArbeidsgiver(PERSON, ORGNR, UTBETALINGSREF, "NY", "", LocalDate.MAX).apply {
            linje(Utbetalingslinjer.Utbetalingslinje(1, "NY", "SPREFAG-IOP", 1.januar, 14.januar, DAGSATS + 1, GRAD - 1, null, null, null, null))
        }

        assertNotEquals(oppdrag1.hashCode(), oppdrag2.hashCode())
    }

    @Test
    fun `Sjekksum er ulik for to oppdrag når datoStatusFom og statuskode er satt på én linje i det ene oppdrag`() {
        val oppdrag1 = Utbetalingslinjer.RefusjonTilArbeidsgiver(PERSON, ORGNR, UTBETALINGSREF, "NY", "", LocalDate.MAX).apply {
            linje(Utbetalingslinjer.Utbetalingslinje(1, "NY", "SPREFAG-IOP", 1.januar, 14.januar, DAGSATS, GRAD, null, null, null, null))
        }
        val oppdrag2 = Utbetalingslinjer.RefusjonTilArbeidsgiver(PERSON, ORGNR, UTBETALINGSREF, "NY", "", LocalDate.MAX).apply {
            linje(Utbetalingslinjer.Utbetalingslinje(1, "NY", "SPREFAG-IOP", 1.januar, 14.januar, DAGSATS, GRAD, null, null, 2.januar, "OPPH"))
        }

        assertNotEquals(oppdrag1.hashCode(), oppdrag2.hashCode())
    }

    @Test
    fun `Sjekksum er lik for to oppdrag som er like`() {
        val oppdrag1 = Utbetalingslinjer.RefusjonTilArbeidsgiver(PERSON, ORGNR, UTBETALINGSREF, "NY", "", LocalDate.MAX).apply {
            linje(Utbetalingslinjer.Utbetalingslinje(1, "NY", "SPREFAG-IOP", 1.januar, 14.januar, DAGSATS, GRAD, null, null, null, null))
        }
        val oppdrag2 = Utbetalingslinjer.RefusjonTilArbeidsgiver(PERSON, ORGNR, UTBETALINGSREF, "NY", "", LocalDate.MAX).apply {
            linje(Utbetalingslinjer.Utbetalingslinje(1, "NY", "SPREFAG-IOP", 1.januar, 14.januar, DAGSATS, GRAD, null, null, null, null))
        }

        assertEquals(oppdrag1.hashCode(), oppdrag2.hashCode())
    }
}
