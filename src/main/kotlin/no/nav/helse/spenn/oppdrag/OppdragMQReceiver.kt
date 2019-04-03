package no.nav.helse.spenn.oppdrag

import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory

import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component

@Component
class OppdragMQReceiver(val jaxb : JAXBOppdrag) {

    private val log = LoggerFactory.getLogger(OppdragMQReceiver::class.java)

    @JmsListener(destination = "\${oppdrag.queue.mottak}")
    fun receiveOppdragKvittering(kvittering: String) {
        log.info(kvittering)
        handleKvittering(jaxb.toOppdrag(kvittering))
    }

    fun handleKvittering(oppdrag : Oppdrag) {
        log.info("Received kvittering for ${oppdrag.mmel.systemId} with status kode ${oppdrag.mmel.kodeMelding}")
        // TODO
    }
}