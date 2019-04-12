package no.nav.helse.spenn.vedtak

import no.nav.helse.integrasjon.okonomi.oppdrag.AksjonsKode
import no.nav.helse.integrasjon.okonomi.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.UtbetalingsLinje
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import java.math.BigDecimal
import java.math.BigInteger

typealias Fodselsnummer = String

interface AktørTilFnrMapper {
    fun tilFnr(aktørId: String): Fodselsnummer
}

private class DummyAktørMapper() : AktørTilFnrMapper {
    override fun tilFnr(aktørId: String): Fodselsnummer = aktørId
}

fun Vedtak.tilOppdrag(mapper: AktørTilFnrMapper = DummyAktørMapper()): UtbetalingsOppdrag = UtbetalingsOppdrag(
        id = søknadId,
        operasjon = AksjonsKode.OPPDATER,
        oppdragGjelder = mapper.tilFnr(aktørId),
        utbetalingsLinje = lagLinjer()
)

private fun Vedtak.lagLinjer(): List<UtbetalingsLinje> =
        vedtaksperioder.mapIndexed { index, periode ->
            periode.fordeling.mapIndexed { fordelingsIndex, fordeling ->
                UtbetalingsLinje(
                        id = "$index/$fordelingsIndex",
                        datoFom = periode.fom,
                        datoTom = periode.tom,
                        sats = BigDecimal(periode.dagsats),
                        satsTypeKode = SatsTypeKode.DAGLIG,
                        utbetalesTil = fordeling.mottager,
                        grad = BigInteger.valueOf(100)
                )
            }
        }.flatten()
