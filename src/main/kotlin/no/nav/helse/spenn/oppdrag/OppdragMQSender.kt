package no.nav.helse.spenn.oppdrag

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jms.core.JmsTemplate
import org.springframework.stereotype.Component

@Component
class OppdragMQSender(val jmsTemplate: JmsTemplate, @Value("\${oppdrag.queue.send}") val sendqueue: String) {

    private val log = LoggerFactory.getLogger(OppdragMQSender::class.java)

    fun sendOppdrag(oppdragXml: String) {
        log.info("sending to $sendqueue: " + oppdragXml)
        jmsTemplate.convertAndSend(sendqueue, oppdragXml)
    }

}