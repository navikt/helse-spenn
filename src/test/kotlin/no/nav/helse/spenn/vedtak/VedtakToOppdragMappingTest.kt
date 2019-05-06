package no.nav.helse.spenn.vedtak

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.integrasjon.okonomi.oppdrag.AksjonsKode
import no.nav.helse.integrasjon.okonomi.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.UtbetalingsLinje
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.util.*

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
                oppdragGjelder = "some_dummy_fnr",
                utbetalingsLinje = listOf(oppdragsLinje)
        )

        val faktisk = vedtak.tilUtbetaling(LocalFnrMapper())
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
}

class LocalFnrMapper : AktørTilFnrMapper {
    override fun tilFnr(aktørId: String): Fodselsnummer = "some_dummy_fnr"
}