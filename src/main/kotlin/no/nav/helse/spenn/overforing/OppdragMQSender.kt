package no.nav.helse.spenn.overforing

import com.ibm.mq.jms.MQQueue
import no.nav.helse.spenn.oppdrag.JAXBOppdrag
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import javax.jms.Connection

class OppdragMQSender(connection: Connection,
                      val sendqueue: String,
                      val replyTo: String,
                      val jaxb : JAXBOppdrag) {

    val jmsSession = connection.createSession()
    val producer = jmsSession
            .createProducer(MQQueue(sendqueue))
    private val log = LoggerFactory.getLogger(OppdragMQSender::class.java)

    fun sendOppdrag(oppdrag: Oppdrag) {
        log.info("sender til Oppdragsystemet for fagsystemId ${oppdrag.oppdrag110.fagsystemId}")
        val oppdragXml = jaxb.fromOppdragToXml(oppdrag)
        log.debug("sending $oppdragXml")
        log.debug("QUEUE: $sendqueue REPLYTO: $replyTo")
        //jmsTemplate.send(sendqueue, createMessage(oppdragXml))
        val message = jmsSession.createTextMessage(oppdragXml)
        message.jmsReplyTo = MQQueue(replyTo)
        producer.send(message)
    }

    fun close() {
        log.info("Closing OppdragMQSender::producer")
        producer.close()
        log.info("Closing OppdragMQSender::jmsSession")
        jmsSession.close() // Hmmm.... men jmsSession er ikke thread-safe?
    }
}