package no.nav.helse.spenn.oppdrag

import java.math.BigDecimal
import java.time.LocalDate

data class Utbetalingsbehov(
    internal val behov: String,
    val utbetalingsreferanse: String,
    internal val oppdragGjelder: String,
    internal val saksbehandler: String,
    internal val utbetaling: Utbetaling?
) {
    data class Utbetaling(
        val organisasjonsnummer: String,
        val maksdato: LocalDate,
        val utbetalingsLinjer: List<Linje>
    )

    data class Linje(
        val sats: BigDecimal,
        val datoFom: LocalDate,
        val datoTom: LocalDate,
        /**
         * Kan registreres med fødselsnummer eller organisasjonsnummer til den enheten som skal motta ubetalingen. Normalt
         * vil dette være den samme som oppdraget gjelder, men kan f.eks være en arbeidsgiver som skal få refundert pengene.
         */
        val utbetalesTil: String
    )
}