package no.nav.helse.spenn.vedtak

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spenn.defaultObjectMapper
import no.nav.helse.spenn.oppdrag.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals

class VedtakToOppdragMappingTest {

    @Test
    fun `mapping av et enkelt vedtak`() {
        val vedtak = etEnkeltVedtak()

        val oppdragsLinje = UtbetalingsLinje(
                id = "0/0",
                sats = BigDecimal.valueOf(1234L),
                satsTypeKode = SatsTypeKode.DAGLIG,
                datoFom = vedtak.vedtaksperioder[0].fom,
                datoTom = vedtak.vedtaksperioder[0].tom,
                utbetalesTil = vedtak.vedtaksperioder[0].fordeling[0].mottager,
                grad = BigInteger.valueOf(100)
        )
        val målbilde = UtbetalingsOppdrag(
                vedtak = defaultObjectMapper.convertValue(vedtak, JsonNode::class.java),
                operasjon = AksjonsKode.OPPDATER,
                oppdragGjelder = "12345678901",
                utbetalingsLinje = listOf(oppdragsLinje)
        )

        val faktisk = vedtak.tilUtbetaling("12345678901")
        Assertions.assertEquals(målbilde, faktisk)
    }

    private fun etEnkeltVedtak(): Vedtak {
        return Vedtak(
                søknadId = UUID.randomUUID(),
                aktørId = "en random aktørid",
                vedtaksperioder = listOf(Vedtaksperiode(
                        fom = LocalDate.of(2020, 1, 15),
                        tom = LocalDate.of(2020, 1, 30),
                        dagsats = 1234,
                        fordeling = listOf(Fordeling(
                                mottager = "897654321",
                                andel = 100
                        ))
                ))
        )
    }

    @Test
    fun testUUIDtoFagID() {
        val uuid = UUID.randomUUID();
        val fagId = uuid.toFagId()
        val decode = fagId.toUUID()
        assertEquals(uuid, decode)
    }
}
