package no.nav.helse.spenn.oppdrag

import no.nav.helse.spenn.dao.OppdragStateService
import no.nav.helse.spenn.dao.OppdragStateStatus
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spenn.metrics.OPPDRAG

import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component

@Component
class OppdragMQReceiver(val jaxb : JAXBOppdrag, val oppdragStateService: OppdragStateService, val meterRegistry: MeterRegistry) {

    private val log = LoggerFactory.getLogger(OppdragMQReceiver::class.java)

    @JmsListener(destination = "\${oppdrag.queue.mottak}")
    fun receiveOppdragResponse(response: String) {
        log.debug(response)
        //rar xml som blir returnert
        val replaced = response.replace("oppdrag xmlns", "ns2:oppdrag xmlns:ns2")
        handleResponse(jaxb.toOppdrag(replaced), replaced)
    }

    private fun handleResponse(oppdrag : Oppdrag, xml: String) {
        log.info("OppdragResponse for ${oppdrag.oppdrag110.fagsystemId}  ${oppdrag.mmel.alvorlighetsgrad}  ${oppdrag.mmel.beskrMelding}")
        val state = oppdragStateService.fetchOppdragStateById(oppdrag.oppdrag110.fagsystemId.toLong())
        state.oppdragResponse = xml
        state.status = mapStatus(oppdrag)
        meterRegistry.counter(OPPDRAG, "status", state.status.name)
        oppdragStateService.saveOppdragState(state)
    }

    private fun mapStatus(oppdrag: Oppdrag): OppdragStateStatus {
        when(oppdrag.mmel.alvorlighetsgrad) {
            "00" -> return OppdragStateStatus.FERDIG
        }
        log.error("FEIL for oppdrag ${oppdrag.oppdrag110.fagsystemId}")
        return OppdragStateStatus.FEIL
    }
}