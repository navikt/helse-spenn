package no.nav.helse.spenn.oppdrag

import no.nav.helse.spenn.testsupport.etEnkeltBehov
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import kotlin.test.assertEquals

class VedtakToOppdragMappingTest {

    @Test
    fun `mapping av et enkelt vedtak`() {
        val råttBehov = etEnkeltBehov()
        val behov = Utbetalingsbehov(råttBehov, "12345678901")

        val oppdragslinje = UtbetalingsLinje(
            id = "1",
            sats = BigDecimal("1000.0"),
            satsTypeKode = SatsTypeKode.DAGLIG,
            datoFom = LocalDate.of(2011, 1, 1),
            datoTom = LocalDate.of(2011, 1, 31),
            utbetalesTil = "123456789",
            grad = BigInteger.valueOf(100)
        )
        val målbilde = UtbetalingsOppdrag(
            behov = råttBehov,
            oppdragGjelder = "12345678901",
            saksbehandler = "Z999999",
            utbetalingsreferanse = "1",
            utbetaling = Utbetaling(
                utbetalingsLinjer = listOf(oppdragslinje),
                organisasjonsnummer = "123456789",
                maksdato = LocalDate.of(2011, 12, 20)
            )
        )

        val mappetBehov = behov.tilUtbetalingsOppdrag()
        assertEquals("1", mappetBehov.utbetaling!!.utbetalingsLinjer.first().id)
        assertEquals(målbilde, mappetBehov)
    }
}
