package no.nav.helse.spenn.simulering

import com.ctc.wstx.exc.WstxEOFException
import no.nav.helse.spenn.UtenforÅpningstidException
import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerBeregningFeilUnderBehandling
import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerFpService
import no.nav.system.os.entiteter.beregningskjema.BeregningStoppnivaa
import no.nav.system.os.entiteter.beregningskjema.BeregningStoppnivaaDetaljer
import no.nav.system.os.entiteter.beregningskjema.BeregningsPeriode
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningResponse
import org.slf4j.LoggerFactory
import java.net.SocketException
import java.time.LocalDate
import javax.net.ssl.SSLException
import javax.xml.ws.WebServiceException
import javax.xml.ws.soap.SOAPFaultException

class SimuleringService(private val simulerFpService: SimulerFpService) {

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val log = LoggerFactory.getLogger(SimuleringService::class.java)
    }

    fun simulerOppdrag(simulerRequest: SimulerBeregningRequest): SimuleringResult {
        return try {
            simulerFpService.simulerBeregning(simulerRequest)?.response?.let {
                mapResponseToResultat(it)
            } ?: SimuleringResult(status = SimuleringStatus.FUNKSJONELL_FEIL, feilmelding = "Fikk ingen respons")
        } catch (e: SimulerBeregningFeilUnderBehandling) {
            log.error("Got error while running Simulering, sjekk sikkerLogg for detaljer", e)
            sikkerLogg.error("Simulering feilet med feilmelding=${e.faultInfo.errorMessage}", e)
            SimuleringResult(status = SimuleringStatus.FUNKSJONELL_FEIL, feilmelding = e.faultInfo.errorMessage)
        } catch (e: SOAPFaultException) {
            if (e.cause is WstxEOFException) {
                throw UtenforÅpningstidException("Oppdrag/UR er stengt", e)
            }
            throw e
        } catch (e: WebServiceException) {
            if (e.cause is SSLException || e.rootCause is SocketException) {
                throw UtenforÅpningstidException("Oppdrag/UR er stengt", e)
            }
            throw e
        }
    }

    private val Throwable.rootCause: Throwable get() {
            var rootCause: Throwable = this
            while (rootCause.cause != null) rootCause = rootCause.cause!!
            return rootCause
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
