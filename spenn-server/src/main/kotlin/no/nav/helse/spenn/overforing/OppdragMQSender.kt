package no.nav.helse.spenn.overforing

import com.ibm.mq.jms.MQQueue
import no.nav.helse.spenn.oppdrag.JAXBOppdrag
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import javax.jms.Connection

class OppdragMQSender(
    connection: Connection,
    private val sendqueue: String,
    private val replyTo: String
) {
    private val jmsSession = connection.createSession()
    private val producer = jmsSession
        .createProducer(jmsSession.createQueue(sendqueue))
    private val log = LoggerFactory.getLogger(OppdragMQSender::class.java)

    fun sendOppdrag(oppdrag: Oppdrag) {
        log.info("sender til Oppdragsystemet for fagsystemId ${oppdrag.oppdrag110.fagsystemId}")
        val oppdragXml = JAXBOppdrag.fromOppdragToXml(oppdrag)
        log.trace("sending $oppdragXml")
        log.trace("QUEUE: $sendqueue REPLYTO: $replyTo")
        val message = jmsSession.createTextMessage(oppdragXml)
        message.jmsReplyTo = MQQueue(replyTo)
        producer.send(message)
    }
}
