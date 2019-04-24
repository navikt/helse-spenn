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
        log.debug(kvittering)
        //rar xml som blir returnert
        val replaced = kvittering.replace("oppdrag xmlns", "ns2:oppdrag xmlns:ns2")
        handleKvittering(jaxb.toOppdrag(replaced))
    }

    private fun handleKvittering(oppdrag : Oppdrag) {
        val kvittering = Kvittering(status=mapStatus(oppdrag), alvorlighetsgrad = oppdrag.mmel.alvorlighetsgrad,
                beskrMelding = oppdrag.mmel.beskrMelding, kodeMelding = oppdrag.mmel.kodeMelding,
                fagsystemId = oppdrag.oppdrag110.fagsystemId)
        log.info("Kvittering for ${kvittering.fagsystemId}  ${kvittering.status} '${kvittering.beskrMelding}'")
        // TODO persist kvittering
    }

    private fun mapStatus(oppdrag: Oppdrag): KvitteringStatus {
        when(oppdrag.mmel.alvorlighetsgrad) {
            "00" -> return KvitteringStatus.OK
            "04" -> return KvitteringStatus.AKSEPTERT_MED_FEILMELDING
        }
        return KvitteringStatus.FEIL
    }
}