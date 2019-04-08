package no.nav.helse.spenn.vedtak

import no.nav.helse.integrasjon.okonomi.oppdrag.AksjonsKode
import no.nav.helse.integrasjon.okonomi.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.OppdragsLinje
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag

fun tilOppdrag(vedtak: Vedtak): UtbetalingsOppdrag = UtbetalingsOppdrag(
        id = vedtak.søknadId,
        operasjon = AksjonsKode.OPPDATER,
        oppdragGjelder = vedtak.fnr,
        oppdragslinje = lagLinjer(vedtak)
)

fun lagLinjer(vedtak: Vedtak): List<OppdragsLinje> =
        vedtak.vedtaksperioder.mapIndexed { index, periode ->
            periode.fordeling.mapIndexed { fordelingsIndex, fordeling ->
                OppdragsLinje(
                        id = "$index/$fordelingsIndex",
                        datoFom = periode.fom,
                        datoTom = periode.tom,
                        sats = periode.dagsats,
                        satsTypeKode = SatsTypeKode.DAGLIG,
                        utbetalesTil = fordeling.mottager
                )
            }
        }.flatten()
