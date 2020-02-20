package no.nav.helse.spenn.oppdrag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spenn.testsupport.etEnkeltBehov
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import kotlin.test.assertEquals

class VedtakToOppdragMappingTest {

    @Test
    fun `mapping av et enkelt vedtak`() {
        val råttBehov = etEnkeltBehov()
        val behov = lagNyttBehov(råttBehov)

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
            behov = råttBehov.toString(),
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

    fun lagNyttBehov(jsonNode: JsonNode) = Utbetalingsbehov(
        behov = jsonNode.toString(),
        utbetalingsreferanse = jsonNode["utbetalingsreferanse"].asText(),
        oppdragGjelder = jsonNode["fødselsnummer"].asText(),
        saksbehandler = jsonNode["saksbehandler"].asText(),
        utbetaling = Utbetalingsbehov.Utbetaling(
            maksdato = jsonNode["maksdato"].asText().let { LocalDate.parse(it) },
            organisasjonsnummer = jsonNode["organisasjonsnummer"].asText(),
            utbetalingsLinjer = jsonNode["utbetalingslinjer"].map { linje ->
                Utbetalingsbehov.Linje(
                    utbetalesTil = jsonNode["organisasjonsnummer"].asText(),
                    sats = BigDecimal(linje["dagsats"].asText()),
                    datoFom = linje["fom"].asText().let { LocalDate.parse(it) },
                    datoTom = linje["tom"].asText().let { LocalDate.parse(it) }
                )
            }
        )
    )
}
