package no.nav.helse.spenn.oppdrag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.system.os.entiteter.oppdragskjema.Attestant
import no.nav.system.os.entiteter.oppdragskjema.Enhet
import no.nav.system.os.entiteter.oppdragskjema.Grad
import no.nav.system.os.entiteter.typer.simpletypes.FradragTillegg
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.Oppdragslinje

import no.nav.helse.spenn.oppdrag.OppdragSkjemaConstants.Companion.APP
import no.nav.helse.spenn.oppdrag.OppdragSkjemaConstants.Companion.BOS
import no.nav.helse.spenn.oppdrag.OppdragSkjemaConstants.Companion.SP_ENHET
import no.nav.helse.spenn.oppdrag.OppdragSkjemaConstants.Companion.toFnrOrOrgnr
import no.nav.helse.spenn.oppdrag.OppdragSkjemaConstants.Companion.toXMLDate
import no.trygdeetaten.skjema.oppdrag.ObjectFactory
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import no.trygdeetaten.skjema.oppdrag.OppdragsLinje150
import no.trygdeetaten.skjema.oppdrag.TfradragTillegg

import java.time.format.DateTimeFormatter

import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate



data class UtbetalingsOppdrag(
        val vedtak: JsonNode? = null,
        val operasjon : AksjonsKode,
        val oppdragGjelder: String, // "angir hvem som saken/vedtaket er registrert på i fagrutinen"
        val utbetalingsLinje : List<UtbetalingsLinje>)

data class UtbetalingsLinje(val id: String, // delytelseId - "fagsystemets entydige identifikasjon av oppdragslinjen"
                            val sats: BigDecimal,
                            val satsTypeKode: SatsTypeKode,
                            val datoFom : LocalDate,
                            val datoTom : LocalDate,
                            val utbetalesTil: String,  // "kan registreres med fødselsnummer eller organisasjonsnummer til den enheten som skal motta ubetalingen. Normalt vil dette være den samme som oppdraget gjelder, men kan f.eks være en arbeidsgiver som skal få refundert pengene."
                            val grad: BigInteger)


private val simFactory = no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.ObjectFactory()
private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val grensesnittFactory = no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.ObjectFactory()

fun UtbetalingsOppdrag.toSimuleringRequest(oppdragId : String): SimulerBeregningRequest {
    var simulerFom = LocalDate.MAX
    var simulerTom = LocalDate.MIN

    val oppdragsEnhet = Enhet().apply {
        enhet = SP_ENHET
        typeEnhet = BOS
        datoEnhetFom = LocalDate.EPOCH.format(formatter)
    }

    val oppdrag = simFactory.createOppdrag().apply {
        kodeEndring = EndringsKode.NY.kode
        kodeFagomraade = FagOmraadekode.SYKEPENGER_REFUSJON.kode
        fagsystemId = oppdragId
        utbetFrekvens = UtbetalingsfrekvensKode.MÅNEDLIG.kode
        oppdragGjelderId = toFnrOrOrgnr(oppdragGjelder)
        datoOppdragGjelderFom = LocalDate.EPOCH.format(formatter)
        saksbehId = APP
        enhet.add(oppdragsEnhet)
        utbetalingsLinje.forEach {
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

fun UtbetalingsOppdrag.toOppdrag(oppdragId: String): Oppdrag {

    val oppdragsEnhet = objectFactory.createOppdragsEnhet120().apply {
        enhet = SP_ENHET
        typeEnhet = BOS
        datoEnhetFom = toXMLDate(LocalDate.EPOCH)
    }

    val oppdrag110 = objectFactory.createOppdrag110().apply {
        kodeAksjon = operasjon.kode
        kodeEndring = EndringsKode.NY.kode
        kodeFagomraade = FagOmraadekode.SYKEPENGER_REFUSJON.kode
        fagsystemId = oppdragId
        utbetFrekvens = UtbetalingsfrekvensKode.MÅNEDLIG.kode
        oppdragGjelderId = toFnrOrOrgnr(oppdragGjelder)
        datoOppdragGjelderFom = toXMLDate(LocalDate.EPOCH)
        saksbehId = APP
        oppdragsEnhet120.add(oppdragsEnhet)
        utbetalingsLinje.forEach {
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
        attestantId = APP
    }

    return  objectFactory.createOppdragsLinje150().apply {
        kodeEndringLinje = EndringsKode.NY.kode
        kodeKlassifik = KlassifiseringsKode.SPREFAG_IOP.kode
        datoVedtakFom = toXMLDate(oppdragslinje.datoFom)
        datoVedtakTom = toXMLDate(oppdragslinje.datoTom)
        delytelseId = oppdragslinje.id
        sats = oppdragslinje.sats
        fradragTillegg = TfradragTillegg.T
        typeSats = oppdragslinje.satsTypeKode.kode
        saksbehId = APP
        utbetalesTilId = toFnrOrOrgnr(oppdragslinje.utbetalesTil)
        brukKjoreplan = "N"
        grad170.add(grad)
        attestant180.add(attestant)

    }
}
