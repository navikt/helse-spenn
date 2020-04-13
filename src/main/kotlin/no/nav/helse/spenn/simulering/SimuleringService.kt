package no.nav.helse.spenn.simulering

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

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val log = LoggerFactory.getLogger(SimuleringService::class.java)
    }

    fun simulerOppdrag(simulerRequest: SimulerBeregningRequest): SimuleringResult {
        return try {
            simulerFpService.simulerBeregning(simulerRequest)?.response?.let {
                mapResponseToResultat(it)
            } ?: SimuleringResult(status = SimuleringStatus.FEIL, feilmelding = "Fikk ingen respons")
        } catch (e: SimulerBeregningFeilUnderBehandling) {
            log.error("Got error while running Simulering, sjekk sikkerLogg for detaljer", e)
            sikkerLogg.error("Simulering feilet med feilmelding=${e.faultInfo.errorMessage}", e)
            SimuleringResult(status = SimuleringStatus.FEIL, feilmelding = e.faultInfo.errorMessage)
        } catch (e: Exception) {
            log.error("Got unexpected error while running Simulering", e)
            SimuleringResult(status = SimuleringStatus.FEIL, feilmelding = e.message ?: "")
        }
    }

    private fun mapResponseToResultat(response: SimulerBeregningResponse) = SimuleringResult(
        status = SimuleringStatus.OK,
        simulering = Simulering(
            gjelderId = response.simulering.gjelderId,
            gjelderNavn = response.simulering.gjelderNavn.trim(),
            datoBeregnet = LocalDate.parse(response.simulering.datoBeregnet),
            totalBelop = response.simulering.belop.intValueExact(),
            periodeList = response.simulering.beregningsPeriode.map { mapBeregningsPeriode(it) }
        )
    )

    private fun mapBeregningsPeriode(periode: BeregningsPeriode) =
        SimulertPeriode(fom = LocalDate.parse(periode.periodeFom),
            tom = LocalDate.parse(periode.periodeTom),
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
            uforegrad = detaljer.uforeGrad.intValueExact(),
            antallSats = detaljer.antallSats.intValueExact(),
            typeSats = detaljer.typeSats.trim(),
            sats = detaljer.sats.intValueExact(),
            belop = detaljer.belop.intValueExact(),
            konto = detaljer.kontoStreng.trim(),
            tilbakeforing = detaljer.isTilbakeforing,
            klassekode = detaljer.klassekode.trim(),
            klassekodeBeskrivelse = detaljer.klasseKodeBeskrivelse.trim(),
            utbetalingsType = detaljer.typeKlasse,
            refunderesOrgNr = detaljer.refunderesOrgNr.removePrefix("00")
        )
}
