package no.nav.helse.spenn.vedtak

import no.nav.helse.spenn.oppdrag.AksjonsKode
import no.nav.helse.spenn.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.UtbetalingsLinje
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

typealias Fodselsnummer = String

data class Utbetalingsbehov(
    val sakskompleksId: UUID,
    val aktørId: String,
    val organisasjonsnummer: String,
    val maksdato: LocalDate,
    val saksbehandler: String,
    val utbetalingslinjer: List<Utbetalingslinje>
) {
    fun tilUtbetaling(fodselsnummer: Fodselsnummer): UtbetalingsOppdrag = UtbetalingsOppdrag(
        behov = this,
        operasjon = AksjonsKode.OPPDATER,
        oppdragGjelder = fodselsnummer,
        utbetalingsLinje = lagLinjer()
    )

    private fun lagLinjer(): List<UtbetalingsLinje> =
        utbetalingslinjer.mapIndexed { index, periode ->
            UtbetalingsLinje(
                id = index.toString(),
                datoFom = periode.fom,
                datoTom = periode.tom,
                sats = periode.dagsats,
                satsTypeKode = SatsTypeKode.DAGLIG,
                utbetalesTil = organisasjonsnummer,
                grad = periode.grad.toBigInteger()
            )
        }
}

data class Utbetalingslinje(
    val grad: Int,
    val dagsats: BigDecimal,
    val fom: LocalDate,
    val tom: LocalDate
)
