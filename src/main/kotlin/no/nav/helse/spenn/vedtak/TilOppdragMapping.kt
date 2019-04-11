package no.nav.helse.spenn.vedtak

import no.nav.helse.integrasjon.okonomi.oppdrag.AksjonsKode
import no.nav.helse.integrasjon.okonomi.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.UtbetalingsLinje
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import java.math.BigDecimal
import java.math.BigInteger

fun tilOppdrag(vedtak: Vedtak): UtbetalingsOppdrag = UtbetalingsOppdrag(
        id = vedtak.søknadId,
        operasjon = AksjonsKode.OPPDATER,
        oppdragGjelder = vedtak.aktørId,
        utbetalingsLinje = lagLinjer(vedtak)
)

fun lagLinjer(vedtak: Vedtak): List<UtbetalingsLinje> =
        vedtak.vedtaksperioder.mapIndexed { index, periode ->
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
