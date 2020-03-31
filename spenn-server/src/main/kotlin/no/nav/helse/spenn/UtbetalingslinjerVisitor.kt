package no.nav.helse.spenn

import no.nav.helse.spenn.Utbetalingslinjer.Utbetalingslinje.RefusjonTilArbeidsgiver
import no.nav.helse.spenn.Utbetalingslinjer.Utbetalingslinje.UtbetalingTilBruker
import java.time.LocalDate

internal interface UtbetalingslinjerVisitor {
    fun preVisitUtbetalingslinjer(
        utbetalingslinjer: Utbetalingslinjer,
        utbetalingsreferanse: String,
        fødselsnummer: String,
        forlengelse: Boolean
    ) {}

    fun visitRefusjonTilArbeidsgiver(
        refusjonTilArbeidsgiver: RefusjonTilArbeidsgiver,
        id: Int,
        organisasjonsnummer: String,
        forlengelse: Boolean,
        fom: LocalDate,
        tom: LocalDate,
        dagsats: Int,
        grad: Int
    ) {}

    fun visitUtbetalingTilBruker(
        utbetalingTilBruker: UtbetalingTilBruker,
        id: Int,
        fødselsnummer: String,
        forlengelse: Boolean,
        fom: LocalDate,
        tom: LocalDate,
        dagsats: Int,
        grad: Int
    ) {}

    fun postVisitUtbetalingslinjer(
        utbetalingslinjer: Utbetalingslinjer,
        utbetalingsreferanse: String,
        fødselsnummer: String,
        forlengelse: Boolean
    ) {}
}
