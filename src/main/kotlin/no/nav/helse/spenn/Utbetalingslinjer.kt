package no.nav.helse.spenn

import java.time.LocalDate

internal sealed class Utbetalingslinjer(
    internal val fagområde: String,
    internal val fagsystemId: String,
    internal val fødselsnummer: String,
    internal val mottaker: String,
    internal val organisasjonsnummer: String,
    internal val endringskode: String,
    internal val saksbehandler: String,
    internal val maksdato: LocalDate?
) : Iterable<Utbetalingslinjer.Utbetalingslinje> {
    private val linjer = mutableListOf<Utbetalingslinje>()

    fun linje(utbetalingslinje: Utbetalingslinje) {
        linjer.add(utbetalingslinje)
    }

    fun isEmpty() = linjer.isEmpty()
    fun førsteDag() = checkNotNull(Utbetalingslinje.førsteDato(linjer)) { "Ingen oppdragslinjer" }
    fun sisteDag() = checkNotNull(Utbetalingslinje.sisteDato(linjer)) { "Ingen oppdragslinjer" }
    fun totalbeløp() = Utbetalingslinje.totalbeløp(linjer)

    override fun iterator() = linjer.toList().listIterator()

    class RefusjonTilArbeidsgiver(
        fødselsnummer: String,
        mottaker: String,
        fagsystemId: String,
        endringskode: String,
        saksbehandler: String,
        maksdato: LocalDate?
    ) : Utbetalingslinjer(
        "SPREF",
        fagsystemId,
        fødselsnummer,
        mottaker,
        mottaker,
        endringskode,
        saksbehandler,
        maksdato
    )

    class UtbetalingTilBruker(
        fødselsnummer: String,
        mottaker: String,
        organisasjonsnummer: String,
        fagsystemId: String,
        endringskode: String,
        saksbehandler: String,
        maksdato: LocalDate?
    ) : Utbetalingslinjer(
        "SP",
        fagsystemId,
        fødselsnummer,
        mottaker,
        organisasjonsnummer,
        endringskode,
        saksbehandler,
        maksdato
    )

    internal class Utbetalingslinje(
        internal val delytelseId: Int,
        internal val endringskode: String,
        internal val klassekode: String,
        internal val fom: LocalDate,
        internal val tom: LocalDate,
        internal val sats: Int,
        internal val grad: Int?,
        internal val refDelytelseId: Int?,
        internal val refFagsystemId: String?,
        internal val datoStatusFom: LocalDate?,
        internal val statuskode: String?,
        internal val satstype: String
    ) {

        internal companion object {
            fun førsteDato(linjer: List<Utbetalingslinje>) = linjer.minByOrNull { it.fom }?.fom
            fun sisteDato(linjer: List<Utbetalingslinje>) = linjer.maxByOrNull { it.tom }?.tom
            fun totalbeløp(linjer: List<Utbetalingslinje>) = linjer.sumBy { it.sats }
        }
    }
}
