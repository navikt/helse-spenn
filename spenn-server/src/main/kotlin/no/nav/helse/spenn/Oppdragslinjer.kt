package no.nav.helse.spenn

import java.time.LocalDate

internal class Oppdragslinjer(internal val utbetalingsreferanse: String,
                              private val organisasjonsnummer: String,
                              internal val fødselsnummer: String,
                              internal val forlengelse: Boolean,
                              private val maksdato: LocalDate
) {

    private val nesteId get() = linjer.size + 1
    private val linjer = mutableListOf<Utbetalingslinje>()

    fun isEmpty() = linjer.isEmpty()

    fun førsteDag() = checkNotNull(Utbetalingslinje.førsteDato(linjer)) { "Ingen oppdragslinjer" }

    fun sisteDag() = checkNotNull(Utbetalingslinje.sisteDato(linjer)) { "Ingen oppdragslinjer" }

    fun accept(visitor: OppdragslinjerVisitor) {
        visitor.preVisitOppdragslinjer(this)
        linjer.forEach { it.accept(visitor) }
        visitor.postVisitOppdragslinjer(this)
    }

    fun refusjonTilArbeidsgiver(fom: LocalDate, tom: LocalDate, dagsats: Int, grad: Int) {
        linjer.add(
            Utbetalingslinje.RefusjonTilArbeidsgiver(
                id = nesteId,
                forlengelse = forlengelse,
                organisasjonsnummer = organisasjonsnummer,
                maksdato = maksdato,
                fom = fom,
                tom = tom,
                dagsats = dagsats,
                grad = grad
            )
        )
    }

    fun utbetalingTilBruker(fom: LocalDate, tom: LocalDate, dagsats: Int, grad: Int) {
        linjer.add(
            Utbetalingslinje.UtbetalingTilBruker(
                id = nesteId,
                forlengelse = forlengelse,
                fødselsnummer = fødselsnummer,
                fom = fom,
                tom = tom,
                dagsats = dagsats,
                grad = grad
            )
        )
    }

    internal sealed class Utbetalingslinje(
        internal val id: Int,
        internal val forlengelse: Boolean,
        internal val fom: LocalDate,
        internal val tom: LocalDate,
        internal val dagsats: Int,
        internal val grad: Int
    ) {

        abstract fun accept(visitor: OppdragslinjerVisitor)

        companion object {
            fun førsteDato(linjer: List<Utbetalingslinje>) = linjer.minBy { it.fom }?.fom
            fun sisteDato(linjer: List<Utbetalingslinje>) = linjer.maxBy { it.tom }?.tom
        }

        class RefusjonTilArbeidsgiver(
            id: Int,
            forlengelse: Boolean,
            internal val organisasjonsnummer: String,
            internal val maksdato: LocalDate,
            fom: LocalDate,
            tom: LocalDate,
            dagsats: Int,
            grad: Int
        ) : Utbetalingslinje(id, forlengelse, fom, tom, dagsats, grad) {
            init {
                require(organisasjonsnummer.length == 9) { "Forventet organisasjonsnummer med lengde 9" }
            }

            override fun accept(visitor: OppdragslinjerVisitor) {
                visitor.visitRefusjonTilArbeidsgiver(this)
            }
        }

        class UtbetalingTilBruker(
            id: Int,
            internal val fødselsnummer: String,
            forlengelse: Boolean,
            fom: LocalDate,
            tom: LocalDate,
            dagsats: Int,
            grad: Int
        ) : Utbetalingslinje(id, forlengelse, fom, tom, dagsats, grad) {
            init {
                require(fødselsnummer.length == 11) { "Forventet fødselsnummer med lengde 11" }
            }

            override fun accept(visitor: OppdragslinjerVisitor) {
                visitor.visitUtbetalingTilBruker(this)
            }
        }
    }
}
