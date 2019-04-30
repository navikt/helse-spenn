package no.nav.helse.spenn.oppdrag

import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory

import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component

@Component
class OppdragMQReceiver(val jaxb : JAXBOppdrag) {

    private val log = LoggerFactory.getLogger(OppdragMQReceiver::class.java)

    @JmsListener(destination = "\${oppdrag.queue.mottak}")
    fun receiveOppdragResponse(response: String) {
        log.debug(response)
        //rar xml som blir returnert
        val replaced = response.replace("oppdrag xmlns", "ns2:oppdrag xmlns:ns2")
        handleResponse(jaxb.toOppdrag(replaced))
    }

    private fun handleResponse(oppdrag : Oppdrag) {
        val response = OppdragResponse(status=mapStatus(oppdrag), alvorlighetsgrad = oppdrag.mmel.alvorlighetsgrad,
                beskrMelding = oppdrag.mmel.beskrMelding, kodeMelding = oppdrag.mmel.kodeMelding,
                fagsystemId = oppdrag.oppdrag110.fagsystemId)
        log.info("OppdragResponse for ${response.fagsystemId}  ${response.status} '${response.beskrMelding}'")
        // TODO persist response
    }

    private fun mapStatus(oppdrag: Oppdrag): OppdragStatus {
        when(oppdrag.mmel.alvorlighetsgrad) {
            "00" -> return OppdragStatus.OK
            "04" -> return OppdragStatus.AKSEPTERT_MED_FEILMELDING
        }
        return OppdragStatus.FEIL
    }
}