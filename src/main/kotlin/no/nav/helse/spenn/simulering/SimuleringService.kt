package no.nav.helse.spenn.simulering

import no.nav.helse.spenn.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.UtbetalingsType
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
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.ws.soap.SOAPFaultException

@Service
class SimuleringService(val simulerFpService: SimulerFpService) {

    companion object {
        private val log = LoggerFactory.getLogger(SimuleringService::class.java)
    }

    fun simulerOppdrag(simulerRequest: SimulerBeregningRequest): SimuleringResult {
        disableCnCheck(simulerFpService)
        dumpXML(simulerRequest)
        return try {
            val response = simulerFpService.simulerBeregning(simulerRequest)
            dumpXML(response)
            mapResponseToResultat(response.response)
        }
        catch (e: SimulerBeregningFeilUnderBehandling) {
            log.error("Got error while running Simulering {}", e.faultInfo.errorMessage)
            SimuleringResult(status = Status.FEIL, feilMelding = e.faultInfo.errorMessage)
        }
        catch (e: SOAPFaultException) {
            log.error("Got soap exception", e)
            log.error("fault ${e.fault} code: ${e.fault.faultCode} message: ${e.fault.faultString}")
            throw e
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
        val tmpTrust = Array<TrustManager>(1){
            TmpTrustManager()
        }
        val conduit = client.conduit as HTTPConduit
        conduit.tlsClientParameters = TLSClientParameters().apply {
            isDisableCNCheck = true
            secureSocketProtocol = "TLS"
            keyManagers = arrayOfNulls(0)
            trustManagers = tmpTrust

        }
    }
}

private class TmpTrustManager : X509TrustManager {

    override fun getAcceptedIssuers(): Array<X509Certificate?> {
        return arrayOfNulls<X509Certificate>(0)
    }

    override fun checkClientTrusted(certs: Array<X509Certificate>,
                                    authType: String) {
    }

    override fun checkServerTrusted(certs: Array<X509Certificate>,
                                    authType: String) {
    }
}