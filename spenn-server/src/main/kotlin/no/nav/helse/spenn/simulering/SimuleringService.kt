package no.nav.helse.spenn.simulering

import no.nav.helse.spenn.metrics
import no.nav.helse.spenn.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.UtbetalingsType
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerBeregningFeilUnderBehandling
import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerFpService
import no.nav.system.os.entiteter.beregningskjema.BeregningStoppnivaa
import no.nav.system.os.entiteter.beregningskjema.BeregningStoppnivaaDetaljer
import no.nav.system.os.entiteter.beregningskjema.BeregningsPeriode
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningResponse
import org.slf4j.LoggerFactory
import java.time.LocalDate

class SimuleringService(private val simulerFpService: SimulerFpService) {

    companion object {
        private val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")
        private val log = LoggerFactory.getLogger(SimuleringService::class.java)
    }

    fun runSimulering(oppdrag: OppdragService.Transaksjon): SimuleringResult {
        return simulerOppdrag(oppdrag.simuleringRequest)
    }

    fun simulerOppdrag(
        simulerRequest: SimulerBeregningRequest
    ): SimuleringResult {
        log.info("simulerer oppdrag")
        sikkerLogg.info("simulering for $simulerRequest")
        return try {
            val response = metrics.timer("simulering").recordCallable {
                simulerFpService.simulerBeregning(simulerRequest)
            }
            mapResponseToResultat(response.response)
        } catch (e: SimulerBeregningFeilUnderBehandling) {
            log.error("Got error while running Simulering, sjekk sikkerLogg for detaljer", e)
            sikkerLogg.error("Simulering for $simulerRequest feilet med feilmelding=${e.faultInfo.errorMessage}", e)
            SimuleringResult(status = SimuleringStatus.FEIL, feilMelding = e.faultInfo.errorMessage)
        } catch (e: Exception) {
            log.error("Got unexpected error while running Simulering", e)
            SimuleringResult(status = SimuleringStatus.FEIL, feilMelding = e.message ?: "")
        }

    }

    private fun mapResponseToResultat(response: SimulerBeregningResponse?) = SimuleringResult(
        status = SimuleringStatus.OK,
        simulering = response?.let { simulerBeregningResponse ->
            Simulering(
                gjelderId = simulerBeregningResponse.simulering.gjelderId,
                gjelderNavn = simulerBeregningResponse.simulering.gjelderNavn.trim(),
                datoBeregnet = LocalDate.parse(simulerBeregningResponse.simulering.datoBeregnet),
                totalBelop = simulerBeregningResponse.simulering.belop,
                periodeList = simulerBeregningResponse.simulering.beregningsPeriode.map { mapBeregningsPeriode(it) })
        }
    )

    private fun mapBeregningsPeriode(periode: BeregningsPeriode) =
        SimulertPeriode(fom = LocalDate.parse(periode.periodeFom), tom = LocalDate.parse(periode.periodeTom),
            utbetaling = periode.beregningStoppnivaa.map { mapBeregningStoppNivaa(it) })

    private fun mapBeregningStoppNivaa(stoppNivaa: BeregningStoppnivaa) =
        Utbetaling(fagSystemId = stoppNivaa.fagsystemId.trim(),
            utbetalesTilNavn = stoppNivaa.utbetalesTilNavn.trim(),
            utbetalesTilId = stoppNivaa.utbetalesTilId.removePrefix("00"),
            forfall = LocalDate.parse(stoppNivaa.forfall),
            feilkonto = stoppNivaa.isFeilkonto,
            detaljer = stoppNivaa.beregningStoppnivaaDetaljer.map { mapDetaljer(it) })

    private fun mapDetaljer(detaljer: BeregningStoppnivaaDetaljer) =
        Detaljer(
            faktiskFom = LocalDate.parse(detaljer.faktiskFom),
            faktiskTom = LocalDate.parse(detaljer.faktiskTom),
            uforegrad = detaljer.uforeGrad,
            antallSats = detaljer.antallSats,
            typeSats = SatsTypeKode.fromKode(detaljer.typeSats.trim()),
            sats = detaljer.sats,
            belop = detaljer.belop,
            konto = detaljer.kontoStreng.trim(),
            tilbakeforing = detaljer.isTilbakeforing,
            klassekode = detaljer.klassekode.trim(),
            klassekodeBeskrivelse = detaljer.klasseKodeBeskrivelse.trim(),
            utbetalingsType = UtbetalingsType.fromKode(detaljer.typeKlasse),
            refunderesOrgNr = detaljer.refunderesOrgNr.removePrefix("00")
        )
}
