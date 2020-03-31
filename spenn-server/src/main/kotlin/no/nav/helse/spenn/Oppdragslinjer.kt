package no.nav.helse.spenn

import no.nav.helse.spenn.core.FagOmraadekode
import no.nav.helse.spenn.oppdrag.*
import no.nav.system.os.entiteter.oppdragskjema.Attestant
import no.nav.system.os.entiteter.oppdragskjema.Enhet
import no.nav.system.os.entiteter.oppdragskjema.Grad
import no.nav.system.os.entiteter.oppdragskjema.RefusjonsInfo
import no.nav.system.os.entiteter.typer.simpletypes.FradragTillegg
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.Oppdrag
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.Oppdragslinje
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal class Oppdragslinjer(private val utbetalingsreferanse: String,
                              private val organisasjonsnummer: String,
                              private val fødselsnummer: String,
                              private val forlengelse: Boolean,
                              private val maksdato: LocalDate
) {

    private companion object {
        private val tidsstempel = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

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

    fun oppdrag(saksbehandler: String) = Oppdrag().apply {
        kodeEndring = if (forlengelse) EndringsKode.UENDRET.kode else EndringsKode.NY.kode
        kodeFagomraade = FagOmraadekode.SYKEPENGER_REFUSJON.kode
        fagsystemId = utbetalingsreferanse
        utbetFrekvens = UtbetalingsfrekvensKode.MÅNEDLIG.kode
        oppdragGjelderId = fødselsnummer
        datoOppdragGjelderFom = LocalDate.EPOCH.format(tidsstempel)
        saksbehId = saksbehandler
        enhet.add(Enhet().apply {
            enhet = OppdragSkjemaConstants.SP_ENHET
            typeEnhet = OppdragSkjemaConstants.BOS
            datoEnhetFom = LocalDate.EPOCH.format(tidsstempel)
        })
        oppdragslinje.addAll(oppdragslinjer(saksbehandler))
    }

    private fun oppdragslinjer(saksbehandler: String): List<Oppdragslinje> = linjer.map { it.somOppdragslinje(saksbehandler) }

    internal sealed class Utbetalingslinje(
        internal val id: Int,
        private val forlengelse: Boolean,
        private val fom: LocalDate,
        private val tom: LocalDate,
        private val dagsats: Int,
        private val grad: Int
    ) {

        abstract fun accept(visitor: OppdragslinjerVisitor)

        protected abstract fun mottaker(oppdragslinje: Oppdragslinje)

        fun somOppdragslinje(saksbehandler: String): Oppdragslinje {
            return Oppdragslinje()
                .apply {
                delytelseId = "$id"
                kodeEndringLinje = if (forlengelse) EndringsKode.ENDRING.kode else EndringsKode.NY.kode
                kodeKlassifik = KlassifiseringsKode.SPREFAG_IOP.kode
                datoVedtakFom = fom.format(tidsstempel)
                datoVedtakTom = tom.format(tidsstempel)
                sats = dagsats.toBigDecimal()
                fradragTillegg = FradragTillegg.T
                typeSats = SatsTypeKode.DAGLIG.kode
                saksbehId = saksbehandler

                mottaker(this)

                brukKjoreplan = "N"
                this.grad.add(Grad().apply {
                    typeGrad = GradTypeKode.UFØREGRAD.kode
                    grad = this@Utbetalingslinje.grad.toBigInteger()
                })
                this.attestant.add(Attestant().apply {
                    attestantId = saksbehandler
                })
            }
        }

        companion object {
            fun førsteDato(linjer: List<Utbetalingslinje>) = linjer.minBy { it.fom }?.fom
            fun sisteDato(linjer: List<Utbetalingslinje>) = linjer.maxBy { it.tom }?.tom
        }

        class RefusjonTilArbeidsgiver(
            id: Int,
            forlengelse: Boolean,
            private val organisasjonsnummer: String,
            private val maksdato: LocalDate,
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

            override fun mottaker(oppdragslinje: Oppdragslinje) {
                oppdragslinje.refusjonsInfo = RefusjonsInfo()
                    .apply {
                    this.refunderesId = "00$organisasjonsnummer"
                    this.datoFom = oppdragslinje.datoVedtakFom
                    this.maksDato = maksdato.format(tidsstempel)
                }
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

            override fun accept(visitor: OppdragslinjerVisitor) {
                visitor.visitUtbetalingTilBruker(this)
            }

            override fun mottaker(oppdragslinje: Oppdragslinje) {
                oppdragslinje.utbetalesTilId = fødselsnummer
            }
        }
    }
}
