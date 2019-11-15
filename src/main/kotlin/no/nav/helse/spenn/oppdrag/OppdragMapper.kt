package no.nav.helse.spenn.oppdrag

import no.nav.helse.spenn.FagOmraadekode
import no.nav.helse.spenn.avstemmingsnokkelFormatter
import no.nav.system.os.entiteter.oppdragskjema.Attestant
import no.nav.system.os.entiteter.oppdragskjema.Enhet
import no.nav.system.os.entiteter.oppdragskjema.Grad
import no.nav.system.os.entiteter.oppdragskjema.RefusjonsInfo
import no.nav.system.os.entiteter.typer.simpletypes.FradragTillegg
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.Oppdragslinje
import no.trygdeetaten.skjema.oppdrag.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter


private val simFactory = no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.ObjectFactory()
private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
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
        fagsystemId = utbetalingsreferanse
        utbetFrekvens = UtbetalingsfrekvensKode.MÅNEDLIG.kode
        oppdragGjelderId = utbetalingsOppdrag.oppdragGjelder
        datoOppdragGjelderFom = LocalDate.EPOCH.format(formatter)
        saksbehId = OppdragSkjemaConstants.APP
        enhet.add(oppdragsEnhet)
        utbetalingsOppdrag.utbetalingsLinje.forEach {
            if (it.datoFom.isBefore(simulerFom)) simulerFom = it.datoFom
            if (it.datoTom.isAfter(simulerTom)) simulerTom = it.datoTom
            oppdragslinje.add(
                mapToSimuleringsOppdragslinje(
                    oppdragslinje = it,
                    maksDato = utbetalingsOppdrag.behov.maksdato,
                    saksbehandler = OppdragSkjemaConstants.APP
                )
            )
        }
    }
    return grensesnittFactory.createSimulerBeregningRequest().apply {
        this.request = simFactory.createSimulerBeregningRequest().apply {
            this.oppdrag = oppdrag
            simuleringsPeriode =
                no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningRequest
                    .SimuleringsPeriode().apply {
                        datoSimulerFom = simulerFom.format(formatter)
                        datoSimulerTom = simulerTom.format(formatter)
                    }
        }
    }

}

private fun mapToSimuleringsOppdragslinje(
    oppdragslinje: UtbetalingsLinje,
    maksDato: LocalDate,
    saksbehandler: String
): Oppdragslinje {
    val erRefusjonTilArbeidsgiver = true

    val grad = Grad().apply {
        typeGrad = GradTypeKode.UFØREGRAD.kode
        grad = oppdragslinje.grad
    }
    val attestant = Attestant().apply {
        attestantId = saksbehandler
    }

    return Oppdragslinje().apply {
        kodeEndringLinje = EndringsKode.NY.kode
        kodeKlassifik = KlassifiseringsKode.SPREFAG_IOP.kode
        datoVedtakFom = oppdragslinje.datoFom.format(formatter)
        datoVedtakTom = oppdragslinje.datoTom.format(formatter)
        delytelseId = oppdragslinje.id
        sats = oppdragslinje.sats
        fradragTillegg = FradragTillegg.T
        typeSats = oppdragslinje.satsTypeKode.kode
        saksbehId = saksbehandler
        if (erRefusjonTilArbeidsgiver) {
            refusjonsInfo = RefusjonsInfo().apply {
                assert(oppdragslinje.utbetalesTil.length == 9)
                this.refunderesId = "00" + oppdragslinje.utbetalesTil
                this.datoFom = datoVedtakFom
                this.maksDato = maksDato.format(formatter)
            }
        } else {
            assert(oppdragslinje.utbetalesTil.length == 11)
            utbetalesTilId = oppdragslinje.utbetalesTil
        }
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
        fagsystemId = utbetalingsOppdrag.behov.utbetalingsreferanse
        utbetFrekvens = UtbetalingsfrekvensKode.MÅNEDLIG.kode
        oppdragGjelderId = utbetalingsOppdrag.oppdragGjelder
        datoOppdragGjelderFom = OppdragSkjemaConstants.toXMLDate(LocalDate.EPOCH)
        saksbehId = utbetalingsOppdrag.behov.saksbehandler
        avstemming115 = objectFactory.createAvstemming115().apply {
            this.nokkelAvstemming = avstemming?.nokkel?.format(avstemmingsnokkelFormatter)
            this.kodeKomponent = KomponentKode.SYKEPENGER.kode
            this.tidspktMelding = avstemming?.nokkel?.format(avstemmingsnokkelFormatter)
        }
        oppdragsEnhet120.add(oppdragsEnhet)
        utbetalingsOppdrag.utbetalingsLinje.forEach {
            oppdragsLinje150.add(
                mapTolinje150(
                    oppdragslinje = it,
                    maksDato = utbetalingsOppdrag.behov.maksdato,
                    saksbehandler = utbetalingsOppdrag.behov.saksbehandler
                )
            )
        }
    }

    return objectFactory.createOppdrag().apply {
        this.oppdrag110 = oppdrag110
    }


}

private fun mapTolinje150(
    oppdragslinje: UtbetalingsLinje,
    maksDato: LocalDate,
    saksbehandler: String
): OppdragsLinje150 {
    val erRefusjonTilArbeidsgiver = true

    val grad = objectFactory.createGrad170().apply {
        typeGrad = GradTypeKode.UFØREGRAD.kode
        grad = oppdragslinje.grad
    }
    val attestant = objectFactory.createAttestant180().apply {
        attestantId = saksbehandler
    }

    return objectFactory.createOppdragsLinje150().apply {
        kodeEndringLinje = EndringsKode.NY.kode
        kodeKlassifik = KlassifiseringsKode.SPREFAG_IOP.kode
        datoVedtakFom = OppdragSkjemaConstants.toXMLDate(oppdragslinje.datoFom)
        datoVedtakTom = OppdragSkjemaConstants.toXMLDate(oppdragslinje.datoTom)
        delytelseId = oppdragslinje.id
        sats = oppdragslinje.sats
        fradragTillegg = TfradragTillegg.T
        typeSats = oppdragslinje.satsTypeKode.kode
        saksbehId = saksbehandler
        if (erRefusjonTilArbeidsgiver) {
            refusjonsinfo156 = Refusjonsinfo156().apply {
                assert(oppdragslinje.utbetalesTil.length == 9)
                this.refunderesId = "00" + oppdragslinje.utbetalesTil
                this.datoFom = datoVedtakFom
                this.maksDato = OppdragSkjemaConstants.toXMLDate(maksDato)
            }
        } else {
            assert(oppdragslinje.utbetalesTil.length == 11)
            utbetalesTilId = oppdragslinje.utbetalesTil
        }
        brukKjoreplan = "N"
        grad170.add(grad)
        attestant180.add(attestant)

    }
}

