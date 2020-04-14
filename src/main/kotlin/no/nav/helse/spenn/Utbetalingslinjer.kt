package no.nav.helse.spenn

import java.time.LocalDate

internal sealed class Utbetalingslinjer(
    internal val fagområde: String,
    internal val fagsystemId: String,
    internal val fødselsnummer: String,
    internal val mottaker: String,
    internal val endringskode: String,
    internal val saksbehandler: String,
    internal val maksdato: LocalDate,
    internal val sjekksum: Int
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
    override fun equals(other: Any?) = other is Utbetalingslinjer && this.hashCode() == other.hashCode()
    override fun hashCode() = sjekksum

    class RefusjonTilArbeidsgiver(
        fødselsnummer: String,
        mottaker: String,
        fagsystemId: String,
        endringskode: String,
        saksbehandler: String,
        maksdato: LocalDate,
        sjekksum: Int
    ) : Utbetalingslinjer("SPREF", fagsystemId, fødselsnummer, mottaker, endringskode, saksbehandler, maksdato, sjekksum)

    class UtbetalingTilBruker(
        fødselsnummer: String,
        mottaker: String,
        fagsystemId: String,
        endringskode: String,
        saksbehandler: String,
        maksdato: LocalDate,
        sjekksum: Int
    ) : Utbetalingslinjer("SP", fagsystemId, fødselsnummer, mottaker, endringskode, saksbehandler, maksdato, sjekksum)

    internal class Utbetalingslinje(
        internal val delytelseId: Int,
        internal val endringskode: String,
        internal val klassekode: String,
        internal val fom: LocalDate,
        internal val tom: LocalDate,
        internal val dagsats: Int,
        internal val grad: Int,
        internal val refDelytelseId: Int?,
        internal val refFagsystemId: String?
    ) {
        internal companion object {
            fun førsteDato(linjer: List<Utbetalingslinje>) = linjer.minBy { it.fom }?.fom
            fun sisteDato(linjer: List<Utbetalingslinje>) = linjer.maxBy { it.tom }?.tom
            fun totalbeløp(linjer: List<Utbetalingslinje>) = linjer.sumBy { it.dagsats }
        }
    }
}
