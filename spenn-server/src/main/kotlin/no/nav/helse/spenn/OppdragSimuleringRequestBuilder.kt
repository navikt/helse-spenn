package no.nav.helse.spenn

import no.nav.helse.spenn.Oppdragslinjer.Utbetalingslinje
import no.nav.helse.spenn.Oppdragslinjer.Utbetalingslinje.RefusjonTilArbeidsgiver
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

internal class OppdragSimuleringRequestBuilder(private val saksbehandler: String, oppdragslinjer: Oppdragslinjer) : OppdragslinjerVisitor {

    private companion object {
        private val tidsstempel = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    private val oppdrag = Oppdrag().apply {
        kodeFagomraade = FagOmraadekode.SYKEPENGER_REFUSJON.kode
        utbetFrekvens = UtbetalingsfrekvensKode.MÅNEDLIG.kode
        datoOppdragGjelderFom = LocalDate.EPOCH.format(tidsstempel)
        saksbehId = saksbehandler
        kodeEndring = if (oppdragslinjer.forlengelse) EndringsKode.UENDRET.kode else EndringsKode.NY.kode
        fagsystemId = oppdragslinjer.utbetalingsreferanse
        oppdragGjelderId = oppdragslinjer.fødselsnummer
        enhet.add(Enhet().apply {
            enhet = OppdragSkjemaConstants.SP_ENHET
            typeEnhet = OppdragSkjemaConstants.BOS
            datoEnhetFom = LocalDate.EPOCH.format(tidsstempel)
        })
    }

    init {
        oppdragslinjer.accept(this)
    }

    override fun visitRefusjonTilArbeidsgiver(refusjonTilArbeidsgiver: RefusjonTilArbeidsgiver) {
        oppdrag.oppdragslinje.add(somOppdragslinje(refusjonTilArbeidsgiver).apply {
            refusjonsInfo = RefusjonsInfo().apply {
                this.refunderesId = "00${refusjonTilArbeidsgiver.organisasjonsnummer}"
                this.maksDato = refusjonTilArbeidsgiver.maksdato.format(tidsstempel)
            }
            refusjonsInfo.datoFom = this.datoVedtakFom
        })
    }

    override fun visitUtbetalingTilBruker(utbetalingTilBruker: Utbetalingslinje.UtbetalingTilBruker) {
        oppdrag.oppdragslinje.add(somOppdragslinje(utbetalingTilBruker).apply {
            this.utbetalesTilId = utbetalingTilBruker.fødselsnummer
        })
    }

    fun build() = oppdrag

    private fun somOppdragslinje(utbetalingslinje: Utbetalingslinje): Oppdragslinje =
        Oppdragslinje().apply {
            delytelseId = "${utbetalingslinje.id}"
            kodeEndringLinje = if (utbetalingslinje.forlengelse) EndringsKode.ENDRING.kode else EndringsKode.NY.kode
            kodeKlassifik = KlassifiseringsKode.SPREFAG_IOP.kode
            datoVedtakFom = utbetalingslinje.fom.format(tidsstempel)
            datoVedtakTom = utbetalingslinje.tom.format(tidsstempel)
            sats = utbetalingslinje.dagsats.toBigDecimal()
            fradragTillegg = FradragTillegg.T
            typeSats = SatsTypeKode.DAGLIG.kode
            saksbehId = saksbehandler
            brukKjoreplan = "N"
            this.grad.add(Grad().apply {
                typeGrad = GradTypeKode.UFØREGRAD.kode
                grad = utbetalingslinje.grad.toBigInteger()
            })
            this.attestant.add(Attestant().apply {
                attestantId = saksbehandler
            })
        }
}
