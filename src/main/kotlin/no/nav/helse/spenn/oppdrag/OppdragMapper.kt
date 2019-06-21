package no.nav.helse.spenn.oppdrag

import no.nav.system.os.entiteter.oppdragskjema.Attestant
import no.nav.system.os.entiteter.oppdragskjema.Enhet
import no.nav.system.os.entiteter.oppdragskjema.Grad
import no.nav.system.os.entiteter.typer.simpletypes.FradragTillegg
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.Oppdragslinje
import no.trygdeetaten.skjema.oppdrag.ObjectFactory
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import no.trygdeetaten.skjema.oppdrag.OppdragsLinje150
import no.trygdeetaten.skjema.oppdrag.TfradragTillegg
import java.time.LocalDate
import java.time.format.DateTimeFormatter


private val simFactory = no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.ObjectFactory()
private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val tidspunktFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSS")
private val grensesnittFactory = no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.ObjectFactory()

fun OppdragStateDTO.toSimuleringRequest(): SimulerBeregningRequest {
    var simulerFom = LocalDate.MAX
    var simulerTom = LocalDate.MIN

    val oppdragsEnhet = Enhet().apply {
        enhet = OppdragSkjemaConstants.SP_ENHET
        typeEnhet = OppdragSkjemaConstants.BOS
        datoEnhetFom = LocalDate.EPOCH.format(formatter)
    }

    val oppdrag = simFactory.createOppdrag().apply {
        kodeEndring = EndringsKode.NY.kode
        kodeFagomraade = FagOmraadekode.SYKEPENGER_REFUSJON.kode
        fagsystemId = id.toString()
        utbetFrekvens = UtbetalingsfrekvensKode.MÅNEDLIG.kode
        oppdragGjelderId = OppdragSkjemaConstants.toFnrOrOrgnr(utbetalingsOppdrag.oppdragGjelder)
        datoOppdragGjelderFom = LocalDate.EPOCH.format(formatter)
        saksbehId = OppdragSkjemaConstants.APP
        enhet.add(oppdragsEnhet)
        utbetalingsOppdrag.utbetalingsLinje.forEach {
            if (it.datoFom.isBefore(simulerFom)) simulerFom = it.datoFom
            if (it.datoTom.isAfter(simulerTom)) simulerTom = it.datoTom
            oppdragslinje.add(mapToOppdragslinje150(it))
        }
    }
    return grensesnittFactory.createSimulerBeregningRequest().apply {
        this.request = simFactory.createSimulerBeregningRequest().apply {
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
        attestantId = OppdragSkjemaConstants.APP
    }

    return  Oppdragslinje().apply {
        kodeEndringLinje = EndringsKode.NY.kode
        kodeKlassifik = KlassifiseringsKode.SPREFAG_IOP.kode
        datoVedtakFom = oppdragslinje.datoFom.format(formatter)
        datoVedtakTom = oppdragslinje.datoTom.format(formatter)
        delytelseId = oppdragslinje.id
        sats = oppdragslinje.sats
        fradragTillegg = FradragTillegg.T
        typeSats = oppdragslinje.satsTypeKode.kode
        saksbehId = OppdragSkjemaConstants.APP
        utbetalesTilId = OppdragSkjemaConstants.toFnrOrOrgnr(oppdragslinje.utbetalesTil)
        brukKjoreplan = "N"
        this.grad.add(grad)
        this.attestant.add(attestant)
    }
}

private val objectFactory = ObjectFactory()

fun OppdragStateDTO.toOppdrag(): Oppdrag {

    val oppdragsEnhet = objectFactory.createOppdragsEnhet120().apply {
        enhet = OppdragSkjemaConstants.SP_ENHET
        typeEnhet = OppdragSkjemaConstants.BOS
        datoEnhetFom = OppdragSkjemaConstants.toXMLDate(LocalDate.EPOCH)
    }

    val oppdrag110 = objectFactory.createOppdrag110().apply {
        kodeAksjon = utbetalingsOppdrag.operasjon.kode
        kodeEndring = EndringsKode.NY.kode
        kodeFagomraade = FagOmraadekode.SYKEPENGER_REFUSJON.kode
        fagsystemId = id.toString()
        utbetFrekvens = UtbetalingsfrekvensKode.MÅNEDLIG.kode
        oppdragGjelderId = OppdragSkjemaConstants.toFnrOrOrgnr(utbetalingsOppdrag.oppdragGjelder)
        datoOppdragGjelderFom = OppdragSkjemaConstants.toXMLDate(LocalDate.EPOCH)
        saksbehId = OppdragSkjemaConstants.APP
        avstemming115 = objectFactory.createAvstemming115().apply {
            this.nokkelAvstemming = avstemmingsNokkel.toString()
            this.kodeKomponent = KomponentKode.SYKEPENGER.kode
            this.tidspktMelding = modified.format(tidspunktFormatter).toString()
        }
        oppdragsEnhet120.add(oppdragsEnhet)
        utbetalingsOppdrag.utbetalingsLinje.forEach {
            oppdragsLinje150.add(mapTolinje150(it))
        }
    }

    return  objectFactory.createOppdrag().apply {
        this.oppdrag110 = oppdrag110
    }


}

private fun mapTolinje150(oppdragslinje : UtbetalingsLinje) : OppdragsLinje150 {
    val grad = objectFactory.createGrad170().apply {
        typeGrad = GradTypeKode.UFØREGRAD.kode
        grad = oppdragslinje.grad
    }
    val attestant = objectFactory.createAttestant180().apply {
        attestantId = OppdragSkjemaConstants.APP
    }

    return  objectFactory.createOppdragsLinje150().apply {
        kodeEndringLinje = EndringsKode.NY.kode
        kodeKlassifik = KlassifiseringsKode.SPREFAG_IOP.kode
        datoVedtakFom = OppdragSkjemaConstants.toXMLDate(oppdragslinje.datoFom)
        datoVedtakTom = OppdragSkjemaConstants.toXMLDate(oppdragslinje.datoTom)
        delytelseId = oppdragslinje.id
        sats = oppdragslinje.sats
        fradragTillegg = TfradragTillegg.T
        typeSats = oppdragslinje.satsTypeKode.kode
        saksbehId = OppdragSkjemaConstants.APP
        utbetalesTilId = OppdragSkjemaConstants.toFnrOrOrgnr(oppdragslinje.utbetalesTil)
        brukKjoreplan = "N"
        grad170.add(grad)
        attestant180.add(attestant)

    }
}
