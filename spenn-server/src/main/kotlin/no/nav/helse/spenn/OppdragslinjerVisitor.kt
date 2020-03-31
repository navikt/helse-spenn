package no.nav.helse.spenn

import no.nav.helse.spenn.Oppdragslinjer.Utbetalingslinje.RefusjonTilArbeidsgiver
import no.nav.helse.spenn.Oppdragslinjer.Utbetalingslinje.UtbetalingTilBruker

internal interface OppdragslinjerVisitor {
    fun preVisitOppdragslinjer(oppdragslinjer: Oppdragslinjer) {}
    fun visitRefusjonTilArbeidsgiver(refusjonTilArbeidsgiver: RefusjonTilArbeidsgiver) {}
    fun visitUtbetalingTilBruker(utbetalingTilBruker: UtbetalingTilBruker) {}
    fun postVisitOppdragslinjer(oppdragslinjer: Oppdragslinjer) {}
}