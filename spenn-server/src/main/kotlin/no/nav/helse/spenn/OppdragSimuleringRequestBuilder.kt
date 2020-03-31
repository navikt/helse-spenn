package no.nav.helse.spenn

import no.nav.helse.spenn.Utbetalingslinjer.Utbetalingslinje
import no.nav.helse.spenn.Utbetalingslinjer.Utbetalingslinje.RefusjonTilArbeidsgiver
import no.nav.helse.spenn.core.FagOmraadekode
import no.nav.helse.spenn.oppdrag.*
import no.nav.system.os.entiteter.oppdragskjema.Attestant
import no.nav.system.os.entiteter.oppdragskjema.Enhet
import no.nav.system.os.entiteter.oppdragskjema.Grad
import no.nav.system.os.entiteter.oppdragskjema.RefusjonsInfo
import no.nav.system.os.entiteter.typer.simpletypes.FradragTillegg
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.Oppdrag
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.Oppdragslinje
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningRequest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest as SimulerBeregningGrensesnittRequest

internal class OppdragSimuleringRequestBuilder(private val saksbehandler: String,
                                               private val maksdato: LocalDate,
                                               private val utbetalingslinjer: Utbetalingslinjer) : UtbetalingslinjerVisitor {

    private companion object {
        private val tidsstempel = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    private val oppdrag = Oppdrag().apply {
        kodeFagomraade = FagOmraadekode.SYKEPENGER_REFUSJON.kode
        utbetFrekvens = UtbetalingsfrekvensKode.MÅNEDLIG.kode
        datoOppdragGjelderFom = LocalDate.EPOCH.format(tidsstempel)
        saksbehId = saksbehandler
        enhet.add(Enhet().apply {
            enhet = OppdragSkjemaConstants.SP_ENHET
            typeEnhet = OppdragSkjemaConstants.BOS
            datoEnhetFom = LocalDate.EPOCH.format(tidsstempel)
        })
    }

    init {
        utbetalingslinjer.accept(this)
    }

    override fun preVisitUtbetalingslinjer(
        utbetalingslinjer: Utbetalingslinjer,
        utbetalingsreferanse: String,
        fødselsnummer: String,
        forlengelse: Boolean
    ) {
        oppdrag.kodeEndring = if (forlengelse) EndringsKode.UENDRET.kode else EndringsKode.NY.kode
        oppdrag.fagsystemId = utbetalingsreferanse
        oppdrag.oppdragGjelderId = fødselsnummer
    }

    override fun visitRefusjonTilArbeidsgiver(
        refusjonTilArbeidsgiver: RefusjonTilArbeidsgiver,
        id: Int,
        organisasjonsnummer: String,
        forlengelse: Boolean,
        fom: LocalDate,
        tom: LocalDate,
        dagsats: Int,
        grad: Int
    ) {
        oppdrag.oppdragslinje.add(somOppdragslinje(id, forlengelse, fom, tom, dagsats, grad).apply {
            refusjonsInfo = RefusjonsInfo().apply {
                this.refunderesId = "00${organisasjonsnummer}"
                this.maksDato = maksdato.format(tidsstempel)
            }
            refusjonsInfo.datoFom = this.datoVedtakFom
        })
    }

    override fun visitUtbetalingTilBruker(
        utbetalingTilBruker: Utbetalingslinje.UtbetalingTilBruker,
        id: Int,
        fødselsnummer: String,
        forlengelse: Boolean,
        fom: LocalDate,
        tom: LocalDate,
        dagsats: Int,
        grad: Int
    ) {
        oppdrag.oppdragslinje.add(somOppdragslinje(id, forlengelse, fom, tom, dagsats, grad).apply {
            this.utbetalesTilId = fødselsnummer
        })
    }

    fun build(): SimulerBeregningGrensesnittRequest {
        return SimulerBeregningGrensesnittRequest().apply {
            request = SimulerBeregningRequest().apply {
                oppdrag = this@OppdragSimuleringRequestBuilder.oppdrag
                simuleringsPeriode = SimulerBeregningRequest.SimuleringsPeriode().apply {
                    datoSimulerFom = utbetalingslinjer.førsteDag().format(tidsstempel)
                    datoSimulerTom = utbetalingslinjer.sisteDag().format(tidsstempel)
                }
            }
        }
    }

    private fun somOppdragslinje(id: Int,
                                 forlengelse: Boolean,
                                 fom: LocalDate,
                                 tom: LocalDate,
                                 dagsats: Int,
                                 grad: Int): Oppdragslinje =
        Oppdragslinje().apply {
            delytelseId = "$id"
            kodeEndringLinje = if (forlengelse) EndringsKode.ENDRING.kode else EndringsKode.NY.kode
            kodeKlassifik = KlassifiseringsKode.SPREFAG_IOP.kode
            datoVedtakFom = fom.format(tidsstempel)
            datoVedtakTom = tom.format(tidsstempel)
            sats = dagsats.toBigDecimal()
            fradragTillegg = FradragTillegg.T
            typeSats = SatsTypeKode.DAGLIG.kode
            saksbehId = saksbehandler
            brukKjoreplan = "N"
            this.grad.add(Grad().apply {
                typeGrad = GradTypeKode.UFØREGRAD.kode
                this.grad = grad.toBigInteger()
            })
            attestant.add(Attestant().apply {
                attestantId = saksbehandler
            })
        }
}
