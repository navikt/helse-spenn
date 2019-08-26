package no.nav.helse.spenn.simulering

import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
import no.nav.helse.spenn.oppdrag.OppdragStateDTO
import no.nav.helse.spenn.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.UtbetalingsType
import no.nav.helse.spenn.oppdrag.toSimuleringRequest
import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerBeregningFeilUnderBehandling
import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerFpService
import no.nav.system.os.entiteter.beregningskjema.BeregningsPeriode
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningResponse
import org.apache.cxf.configuration.jsse.TLSClientParameters
import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.transport.http.HTTPConduit
import no.nav.system.os.entiteter.beregningskjema.BeregningStoppnivaa
import no.nav.system.os.entiteter.beregningskjema.BeregningStoppnivaaDetaljer
import java.time.LocalDate

import org.slf4j.LoggerFactory

import org.springframework.stereotype.Service
import java.io.StringWriter
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller

@Service
class SimuleringService(val simulerFpService: SimulerFpService) {

    companion object {
        private val log = LoggerFactory.getLogger(SimuleringService::class.java)
    }

    fun runSimulering(oppdrag: OppdragStateDTO): OppdragStateDTO {
        log.info("simulering for ${oppdrag.soknadId}")
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
        return SimuleringResult(status=Status.FEIL,feilMelding = "Tomt vedtak")
    }

    fun simulerOppdrag(simulerRequest: SimulerBeregningRequest): SimuleringResult {
        disableCnCheck(simulerFpService)
        dumpXML(simulerRequest)
        return try {
            val response = simulerFpService.simulerBeregning(simulerRequest)
            mapResponseToResultat(response.response)
        }
        catch (e: SimulerBeregningFeilUnderBehandling) {
            log.error("Got error while running Simulering {}", e.faultInfo.errorMessage)
            SimuleringResult(status = Status.FEIL, feilMelding = e.faultInfo.errorMessage)
        }

    }

    private fun dumpXML(type: Any) {
        val jaxbContext = JAXBContext.newInstance(type.javaClass)
        val marshaller = jaxbContext.createMarshaller()
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        val stringWriter = StringWriter()
        marshaller.marshal(type, stringWriter)
        log.debug(stringWriter.toString())
    }

    private fun mapResponseToResultat(response: SimulerBeregningResponse) : SimuleringResult {
        val simulering = response.simulering

        return SimuleringResult(status = Status.OK, mottaker =
        Mottaker(gjelderId = simulering.gjelderId, gjelderNavn = simulering.gjelderNavn.trim(),
                datoBeregnet = simulering.datoBeregnet, totalBelop = simulering.belop,
                periodeList = mapPeriodeList(simulering.beregningsPeriode)))
    }

    private fun mapPeriodeList(beregningsPeriode: List<BeregningsPeriode>): List<Periode> {
        return beregningsPeriode.flatMap{ it.beregningStoppnivaa.flatMap{
            it.beregningStoppnivaaDetaljer.map { detaljer ->
                mapPeriode(it, detaljer)
            }
        }}
    }

    private fun mapPeriode(stoppNivaa: BeregningStoppnivaa, detaljer: BeregningStoppnivaaDetaljer): Periode {
        return Periode(id = detaljer.delytelseId.trim(), belop = detaljer.belop, sats = detaljer.sats, typeSats = SatsTypeKode.fromKode(detaljer.typeSats.trim()),
                antallSats = detaljer.antallSats, faktiskFom = LocalDate.parse(detaljer.faktiskFom),
                faktiskTom = LocalDate.parse(detaljer.faktiskTom), forfall = LocalDate.parse(stoppNivaa.forfall),
                oppdragsId = stoppNivaa.oppdragsId, konto = detaljer.kontoStreng.trim(), utbetalesTilId = stoppNivaa.utbetalesTilId,
                utbetalesTilNavn = stoppNivaa.utbetalesTilNavn.trim(), uforegrad = detaljer.uforeGrad,
                utbetalingsType = UtbetalingsType.fromKode(detaljer.typeKlasse.trim()))
    }


    private fun disableCnCheck(port: SimulerFpService) {
        val client = ClientProxy.getClient(port)
        val conduit = client.conduit as HTTPConduit
        conduit.tlsClientParameters = TLSClientParameters().apply {
            isDisableCNCheck = true
        }
    }


}