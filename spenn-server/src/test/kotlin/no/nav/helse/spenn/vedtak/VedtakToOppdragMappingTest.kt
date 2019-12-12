package no.nav.helse.spenn.vedtak

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.spenn.etEnkeltBehov
import no.nav.helse.spenn.oppdrag.AksjonsKode
import no.nav.helse.spenn.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.UtbetalingsLinje
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
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
            behov = behov,
            operasjon = AksjonsKode.OPPDATER,
            oppdragGjelder = "12345678901",
            utbetalingsLinje = listOf(oppdragsLinje),
            saksbehandler = "Z999999",
            organisasjonsnummer = "897654321",
            maksdato = LocalDate.now().plusYears(1),
            utbetalingsreferanse = "1"
        )

        val faktisk = SpennOppdragFactory.lagOppdragFraBehov(behov, "12345678901")
        Assertions.assertEquals(målbilde, faktisk)
    }

    @Test
    fun testFoersteLinjeIMappetUtbetalingslinjeSkalHaId_lik_1() {
        val behov = ObjectMapper().readTree(this.javaClass.getResource("/et_utbetalingsbehov.json"))
        val utbetalingsOppdrag = SpennOppdragFactory.lagOppdragFraBehov(behov, "01010112345")
        assertEquals(1.toString(), utbetalingsOppdrag.utbetalingsLinje.first().id)
    }
}
