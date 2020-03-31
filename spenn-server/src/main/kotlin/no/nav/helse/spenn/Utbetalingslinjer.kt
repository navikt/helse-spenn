package no.nav.helse.spenn

import java.time.LocalDate

internal class Utbetalingslinjer(private val utbetalingsreferanse: String,
                                 private val organisasjonsnummer: String,
                                 private val fødselsnummer: String,
                                 private val forlengelse: Boolean
) {

    private val nesteId get() = linjer.size + 1
    private val linjer = mutableListOf<Utbetalingslinje>()

    fun isEmpty() = linjer.isEmpty()

    fun førsteDag() = checkNotNull(Utbetalingslinje.førsteDato(linjer)) { "Ingen oppdragslinjer" }

    fun sisteDag() = checkNotNull(Utbetalingslinje.sisteDato(linjer)) { "Ingen oppdragslinjer" }

    fun accept(visitor: UtbetalingslinjerVisitor) {
        visitor.preVisitUtbetalingslinjer(this, utbetalingsreferanse, fødselsnummer, forlengelse)
        linjer.forEach { it.accept(visitor) }
        visitor.postVisitUtbetalingslinjer(this, utbetalingsreferanse, fødselsnummer, forlengelse)
    }

    fun refusjonTilArbeidsgiver(fom: LocalDate, tom: LocalDate, dagsats: Int, grad: Int) {
        linjer.add(
            Utbetalingslinje.RefusjonTilArbeidsgiver(
                id = nesteId,
                forlengelse = forlengelse,
                organisasjonsnummer = organisasjonsnummer,
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
        protected val id: Int,
        protected val forlengelse: Boolean,
        protected val fom: LocalDate,
        protected val tom: LocalDate,
        protected val dagsats: Int,
        protected val grad: Int
    ) {

        abstract fun accept(visitor: UtbetalingslinjerVisitor)

        companion object {
            fun førsteDato(linjer: List<Utbetalingslinje>) = linjer.minBy { it.fom }?.fom
            fun sisteDato(linjer: List<Utbetalingslinje>) = linjer.maxBy { it.tom }?.tom
        }

        class RefusjonTilArbeidsgiver(
            id: Int,
            forlengelse: Boolean,
            private val organisasjonsnummer: String,
            fom: LocalDate,
            tom: LocalDate,
            dagsats: Int,
            grad: Int
        ) : Utbetalingslinje(id, forlengelse, fom, tom, dagsats, grad) {
            init {
                require(organisasjonsnummer.length == 9) { "Forventet organisasjonsnummer med lengde 9" }
            }

            override fun accept(visitor: UtbetalingslinjerVisitor) {
                visitor.visitRefusjonTilArbeidsgiver(this, id, organisasjonsnummer, forlengelse, fom, tom, dagsats, grad)
            }
        }

        class UtbetalingTilBruker(
            id: Int,
            private val fødselsnummer: String,
            forlengelse: Boolean,
            fom: LocalDate,
            tom: LocalDate,
            dagsats: Int,
            grad: Int
        ) : Utbetalingslinje(id, forlengelse, fom, tom, dagsats, grad) {
            init {
                require(fødselsnummer.length == 11) { "Forventet fødselsnummer med lengde 11" }
            }

            override fun accept(visitor: UtbetalingslinjerVisitor) {
                visitor.visitUtbetalingTilBruker(this, id, fødselsnummer, forlengelse, fom, tom, dagsats, grad)
            }
        }
    }
}
