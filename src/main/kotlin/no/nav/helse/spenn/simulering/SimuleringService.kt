package no.nav.helse.spenn.simulering

import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerFpService
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.ObjectFactory
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningRequest.SimuleringsPeriode
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.Oppdrag

import org.slf4j.LoggerFactory

import org.springframework.stereotype.Service

@Service
class SimuleringService(private val simulerFpService: SimulerFpService) {

    private val log = LoggerFactory.getLogger(SimuleringService::class.java)


    fun simulerOppdrag(simulerRequest: SimulerBeregningRequest) {
        val beregningResponse = simulerFpService.simulerBeregning(simulerRequest)
        log.info(beregningResponse.response.infomelding.beskrMelding)
        log.info(beregningResponse.response.simulering.belop.toPlainString())
    }


}