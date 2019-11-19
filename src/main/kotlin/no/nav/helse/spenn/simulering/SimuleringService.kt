package no.nav.helse.spenn.simulering

import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spenn.oppdrag.OppdragStateDTO
import no.nav.helse.spenn.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.UtbetalingsType
import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
import no.nav.helse.spenn.oppdrag.toSimuleringRequest
import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerBeregningFeilUnderBehandling
import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerFpService
import no.nav.system.os.entiteter.beregningskjema.BeregningStoppnivaa
import no.nav.system.os.entiteter.beregningskjema.BeregningStoppnivaaDetaljer
import no.nav.system.os.entiteter.beregningskjema.BeregningsPeriode
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningResponse
import org.apache.cxf.configuration.jsse.TLSClientParameters
import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.transport.http.HTTPConduit
import org.slf4j.LoggerFactory
import java.time.LocalDate

class SimuleringService(val simulerFpService: SimulerFpService,
                        val meterRegistry: MeterRegistry) {

    companion object {
        private val log = LoggerFactory.getLogger(SimuleringService::class.java)
    }

    fun runSimulering(oppdrag: OppdragStateDTO): OppdragStateDTO {
        log.info("simulering for sakskompleks ${oppdrag.sakskompleksId} med utbetalingsreferanse ${oppdrag.utbetalingsreferanse}" )

        val result = callSimulering(oppdrag)
        val status = when (result.status) {
            Status.OK -> OppdragStateStatus.SIMULERING_OK
            else -> OppdragStateStatus.SIMULERING_FEIL
        }
        return oppdrag.copy(simuleringResult = result, status = status)
    }

    private fun callSimulering(oppdrag: OppdragStateDTO): SimuleringResult {
        if (oppdrag.utbetalingsOppdrag.utbetalingsLinje.isNotEmpty()) {
            return simulerOppdrag(oppdrag.toSimuleringRequest())
        }
        log.error("Kan ikke simulere betaling: Mangler utbetalingslinjer")
        return SimuleringResult(status=Status.FEIL,feilMelding = "Tomt vedtak")
    }

    fun simulerOppdrag(simulerRequest: SimulerBeregningRequest): SimuleringResult {
        disableCnCheck(simulerFpService)
        return try {
            val response = meterRegistry.timer("simulering").recordCallable {
                simulerFpService.simulerBeregning(simulerRequest)
            }
            mapResponseToResultat(response.response)
        }
        catch (e: SimulerBeregningFeilUnderBehandling) { // TODO
            log.error("Got error while running Simulering {}", e.faultInfo.errorMessage)
            SimuleringResult(status = Status.FEIL, feilMelding = e.faultInfo.errorMessage)
        }
        catch (e: Exception) { // TODO
            log.error("Got unexpected error while running Simulering {}", e)
            SimuleringResult(status = Status.FEIL, feilMelding = e.message?:"")
        }

    }

    private fun mapResponseToResultat(response: SimulerBeregningResponse) : SimuleringResult {
        val beregning = response.simulering
        return SimuleringResult(status = Status.OK, simulering = Simulering(
                gjelderId = beregning.gjelderId, gjelderNavn = beregning.gjelderNavn.trim(), datoBeregnet = LocalDate.parse(beregning.datoBeregnet),
                totalBelop = beregning.belop, periodeList = beregning.beregningsPeriode.map {mapBeregningsPeriode(it)}))
    }

    private fun mapBeregningsPeriode(periode: BeregningsPeriode): SimulertPeriode {
        return SimulertPeriode(fom = LocalDate.parse(periode.periodeFom), tom = LocalDate.parse(periode.periodeTom),
                utbetaling = periode.beregningStoppnivaa.map {mapBeregningStoppNivaa(it)})
    }

    private fun mapBeregningStoppNivaa(stoppNivaa: BeregningStoppnivaa): Utbetaling {
        return Utbetaling(fagSystemId = stoppNivaa.fagsystemId.trim(), utbetalesTilNavn = stoppNivaa.utbetalesTilNavn.trim(),
                utbetalesTilId = stoppNivaa.utbetalesTilId, forfall = LocalDate.parse(stoppNivaa.forfall),
                feilkonto = stoppNivaa.isFeilkonto,
                detaljer = stoppNivaa.beregningStoppnivaaDetaljer.map {mapDetaljer(it)})
    }

    private fun mapDetaljer(detaljer: BeregningStoppnivaaDetaljer): Detaljer {
        return Detaljer(faktiskFom = LocalDate.parse(detaljer.faktiskFom), faktiskTom = LocalDate.parse(detaljer.faktiskTom),
                uforegrad = detaljer.uforeGrad, antallSats = detaljer.antallSats, typeSats = SatsTypeKode.fromKode(detaljer.typeSats.trim()),
                sats = detaljer.sats, belop = detaljer.belop, konto = detaljer.kontoStreng.trim(), tilbakeforing = detaljer.isTilbakeforing,
                klassekode = detaljer.klassekode.trim(), klassekodeBeskrivelse = detaljer.klasseKodeBeskrivelse.trim(),
                utbetalingsType = UtbetalingsType.fromKode(detaljer.typeKlasse), refunderesOrgNr = detaljer.refunderesOrgNr)
    }

    private fun disableCnCheck(port: SimulerFpService) {
        val client = ClientProxy.getClient(port)
        val conduit = client.conduit as HTTPConduit
        conduit.tlsClientParameters = TLSClientParameters().apply {
            isDisableCNCheck = true
        }
    }

}