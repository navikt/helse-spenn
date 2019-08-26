package no.nav.helse.spenn.overforing


import no.nav.helse.spenn.oppdrag.JAXBOppdrag
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jms.core.JmsTemplate
import org.springframework.jms.core.MessageCreator
import org.springframework.stereotype.Component


@Component
class OppdragMQSender(val jmsTemplate: JmsTemplate, @Value("\${oppdrag.queue.send}") val sendqueue: String,
                      @Value("\${oppdrag.queue.mottak}") val replyTo: String,
                      val jaxb : JAXBOppdrag) {

    private val log = LoggerFactory.getLogger(OppdragMQSender::class.java)


    fun sendOppdrag(oppdrag: Oppdrag) {
        val oppdragXml = jaxb.fromOppdragToXml(oppdrag)
        log.debug("sending $oppdragXml")
        log.debug("QUEUE: $sendqueue REPLYTO: $replyTo")
        jmsTemplate.send(sendqueue, createMessage(oppdragXml))
    }

    private fun createMessage(message: String): MessageCreator {
        return MessageCreator {
            it.createTextMessage(message).apply {
                jmsReplyTo = it.createQueue(replyTo)
            }
        }
    }
}