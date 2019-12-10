package no.nav.helse.spenn.vedtak

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.helse.spenn.oppdrag.AksjonsKode
import no.nav.helse.spenn.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.UtbetalingsLinje
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import no.nav.system.os.entiteter.oppdragskjema.Avstemmingsnokkel
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

typealias Fodselsnummer = String

@JsonIgnoreProperties(ignoreUnknown = true)
data class Utbetalingsbehov(
    val sakskompleksId: UUID,
    val utbetalingsreferanse: String,
    val aktørId: String,
    val organisasjonsnummer: String,
    val maksdato: LocalDate,
    val saksbehandler: String,
    val utbetalingslinjer: List<Utbetalingslinje>,
    val løsning: Løsning? = null
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
                id = (index + 1).toString(),
                datoFom = periode.fom,
                datoTom = periode.tom,
                sats = periode.dagsats,
                satsTypeKode = SatsTypeKode.DAGLIG,
                utbetalesTil = organisasjonsnummer,
                grad = 100.toBigInteger() //periode.grad.toBigInteger()
            )
        }
}

data class Løsning(val avstemmingsnokkel: String)

data class Utbetalingslinje(
    val grad: Int,
    val dagsats: BigDecimal,
    val fom: LocalDate,
    val tom: LocalDate
)
