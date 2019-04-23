package no.nav.helse.spenn.simulering

import no.nav.helse.integrasjon.okonomi.oppdrag.EndringsKode
import no.nav.helse.integrasjon.okonomi.oppdrag.GradTypeKode
import no.nav.helse.integrasjon.okonomi.oppdrag.OppdragSkjemaConstants.Companion.APP
import no.nav.helse.integrasjon.okonomi.oppdrag.OppdragSkjemaConstants.Companion.BOS
import no.nav.helse.integrasjon.okonomi.oppdrag.OppdragSkjemaConstants.Companion.KOMPONENT_KODE
import no.nav.helse.integrasjon.okonomi.oppdrag.OppdragSkjemaConstants.Companion.SP
import no.nav.helse.integrasjon.okonomi.oppdrag.OppdragSkjemaConstants.Companion.SP_ENHET
import no.nav.helse.integrasjon.okonomi.oppdrag.OppdragSkjemaConstants.Companion.toFnrOrOrgnr
import no.nav.helse.integrasjon.okonomi.oppdrag.UtbetalingsfrekvensKode
import no.nav.helse.spenn.oppdrag.UtbetalingsLinje
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import no.nav.system.os.entiteter.oppdragskjema.Attestant
import no.nav.system.os.entiteter.oppdragskjema.Enhet
import no.nav.system.os.entiteter.oppdragskjema.Grad
import no.nav.system.os.entiteter.typer.simpletypes.FradragTillegg
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.ObjectFactory
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.Oppdragslinje
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class OppdragMapperForSimulering() {


    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val objectFactory = ObjectFactory()
    private val grensesnittFactory = no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.ObjectFactory()

    fun mapOppdragToSimuleringRequest(utbetaling : UtbetalingsOppdrag): SimulerBeregningRequest {
        var simulerFom = LocalDate.MAX
        var simulerTom = LocalDate.MIN

        val oppdragsEnhet = Enhet().apply {
            enhet = SP_ENHET
            typeEnhet = BOS
            datoEnhetFom = LocalDate.EPOCH.format(formatter)
        }

        val oppdrag = objectFactory.createOppdrag().apply {
            kodeEndring = EndringsKode.NY.kode
            kodeFagomraade = SP
            fagsystemId = utbetaling.id
            utbetFrekvens = UtbetalingsfrekvensKode.MÅNEDLIG.kode
            oppdragGjelderId = toFnrOrOrgnr(utbetaling.oppdragGjelder)
            datoOppdragGjelderFom = LocalDate.EPOCH.format(formatter)
            saksbehId = APP
            enhet.add(oppdragsEnhet)
            utbetaling.utbetalingsLinje.forEach {
                if (it.datoFom.isBefore(simulerFom)) simulerFom = it.datoFom
                if (it.datoTom.isAfter(simulerTom)) simulerTom = it.datoTom
                oppdragslinje.add(mapToOppdragslinje150(it))
            }
        }
        return grensesnittFactory.createSimulerBeregningRequest().apply {
            this.request = objectFactory.createSimulerBeregningRequest().apply {
                this.oppdrag = oppdrag
                simuleringsPeriode = no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningRequest
                        .SimuleringsPeriode().apply {
                            datoSimulerFom = simulerFom.format(formatter)
                            datoSimulerTom = simulerTom.format(formatter)
                        }
            }
        }

    }

    private fun mapToOppdragslinje150(oppdragslinje : UtbetalingsLinje) : Oppdragslinje {
        val grad = Grad().apply {
            typeGrad = GradTypeKode.UFØREGRAD.kode
            grad = oppdragslinje.grad
        }
        val attestant = Attestant().apply {
            attestantId = APP
        }

        return  Oppdragslinje().apply {
            kodeEndringLinje = EndringsKode.NY.kode
            kodeKlassifik = KOMPONENT_KODE
            datoVedtakFom = oppdragslinje.datoFom.format(formatter)
            datoVedtakTom = oppdragslinje.datoTom.format(formatter)
            delytelseId = oppdragslinje.id
            sats = oppdragslinje.sats
            fradragTillegg = FradragTillegg.T
            typeSats = oppdragslinje.satsTypeKode.kode
            saksbehId = APP
            utbetalesTilId = toFnrOrOrgnr(oppdragslinje.utbetalesTil)
            brukKjoreplan = "N"
            this.grad.add(grad)
            this.attestant.add(attestant)
        }
    }

}