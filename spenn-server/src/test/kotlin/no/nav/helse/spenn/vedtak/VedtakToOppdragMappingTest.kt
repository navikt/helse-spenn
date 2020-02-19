package no.nav.helse.spenn.vedtak

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.spenn.UtbetalingLøser.Companion.lagOppdragFraBehov
import no.nav.helse.spenn.etEnkeltBehov
import no.nav.helse.spenn.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.Utbetaling
import no.nav.helse.spenn.oppdrag.UtbetalingsLinje
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import no.nav.helse.spenn.toOppdragsbehov
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import kotlin.test.assertEquals

class VedtakToOppdragMappingTest {

    @Test
    fun `mapping av et enkelt vedtak`() {
        val behov = etEnkeltBehov(maksdato = LocalDate.now().plusYears(1))

        val oppdragsLinje = UtbetalingsLinje(
            id = "1",
            sats = BigDecimal("1234.0"),
            satsTypeKode = SatsTypeKode.DAGLIG,
            datoFom = LocalDate.of(2020, 1, 15),
            datoTom = LocalDate.of(2020, 1, 30),
            utbetalesTil = "897654321",
            grad = BigInteger.valueOf(100)
        )
        val målbilde = UtbetalingsOppdrag(
            behov = behov.toString(),
            oppdragGjelder = "12345678901",
            saksbehandler = "Z999999",
            utbetalingsreferanse = "1",
            utbetaling = Utbetaling(
                utbetalingsLinjer = listOf(oppdragsLinje),
                organisasjonsnummer = "897654321",
                maksdato = LocalDate.now().plusYears(1)
            )
        )

        val faktisk = lagOppdragFraBehov(behov.toOppdragsbehov())
        Assertions.assertEquals(målbilde, faktisk)
    }

    @Test
    fun testFoersteLinjeIMappetUtbetalingslinjeSkalHaId_lik_1() {
        val behov = ObjectMapper().readTree(this.javaClass.getResource("/et_utbetalingsbehov.json"))
        val utbetalingsOppdrag = lagOppdragFraBehov(behov.toOppdragsbehov())
        assertEquals(1.toString(), utbetalingsOppdrag.utbetaling!!.utbetalingsLinjer.first().id)
    }
}
