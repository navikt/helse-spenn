package no.nav.helse.spenn.vedtak

import no.nav.helse.spenn.etEnkeltVedtak
import no.nav.helse.spenn.oppdrag.*
import no.nav.helse.spenn.oppdrag.SatsTypeKode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
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
                vedtak = vedtak,
                operasjon = AksjonsKode.OPPDATER,
                oppdragGjelder = "12345678901",
                utbetalingsLinje = listOf(oppdragsLinje)
        )

        val faktisk = vedtak.tilUtbetaling("12345678901")
        Assertions.assertEquals(målbilde, faktisk)
    }

    @Test
    fun testUUIDtoFagID() {
        val uuid = UUID.randomUUID();
        val fagId = uuid.toFagId()
        val decode = fagId.toUUID()
        assertEquals(uuid, decode)
    }
}
