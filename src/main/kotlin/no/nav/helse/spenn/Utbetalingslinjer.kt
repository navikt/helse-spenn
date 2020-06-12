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
    override fun equals(other: Any?) = other is Utbetalingslinjer && this.hashCode() == other.hashCode()
    override fun hashCode() = linjer.hashCode() * 67 +
            mottaker.hashCode() * 71 +
            organisasjonsnummer.hashCode() * 73 +
            fødselsnummer.hashCode()

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
    ) : Utbetalingslinjer("SP", fagsystemId, fødselsnummer, mottaker, organisasjonsnummer, endringskode, saksbehandler, maksdato)

    internal class Utbetalingslinje(
        internal val delytelseId: Int,
        internal val endringskode: String,
        internal val klassekode: String,
        internal val fom: LocalDate,
        internal val tom: LocalDate,
        internal val dagsats: Int,
        internal val grad: Int,
        internal val refDelytelseId: Int?,
        internal val refFagsystemId: String?,
        internal val datoStatusFom: LocalDate?,
        internal val statuskode: String?
    ) {
        internal companion object {
            fun førsteDato(linjer: List<Utbetalingslinje>) = linjer.minBy { it.fom }?.fom
            fun sisteDato(linjer: List<Utbetalingslinje>) = linjer.maxBy { it.tom }?.tom
            fun totalbeløp(linjer: List<Utbetalingslinje>) = linjer.sumBy { it.dagsats }
        }

        override fun hashCode() = fom.hashCode() * 37 +
                tom.hashCode() * 17 +
                dagsats.hashCode() * 41 +
                grad.hashCode() * 61 +
                endringskode.hashCode() * 59 +
                datoStatusFom.hashCode() * 23

        override fun equals(other: Any?) = other is Utbetalingslinje && this.hashCode() == other.hashCode()
    }
}
