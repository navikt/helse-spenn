package no.nav.helse.spenn.overforing


import com.ibm.mq.jms.MQQueue
import no.nav.helse.spenn.oppdrag.JAXBOppdrag
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import javax.jms.Connection
import javax.jms.Session

/*import org.springframework.beans.factory.annotation.Value
import org.springframework.jms.core.JmsTemplate
import org.springframework.jms.core.MessageCreator
import org.springframework.stereotype.Component*/


//@Component
class OppdragMQSender(connection: Connection, /*@Value("\${oppdrag.queue.send}")*/ val sendqueue: String,
                      /*@Value("\${oppdrag.queue.mottak}")*/ val replyTo: String,
                      val jaxb : JAXBOppdrag) {

    val jmsSession = connection.createSession()
    val producer = jmsSession
            .createProducer(MQQueue(sendqueue))
    private val log = LoggerFactory.getLogger(OppdragMQSender::class.java)

    /*init {
        Runtime.getRuntime().addShutdownHook(Thread {
            close()
        })
    }*/


    fun sendOppdrag(oppdrag: Oppdrag) {
        val oppdragXml = jaxb.fromOppdragToXml(oppdrag)
        log.debug("sending $oppdragXml")
        log.debug("QUEUE: $sendqueue REPLYTO: $replyTo")
        //jmsTemplate.send(sendqueue, createMessage(oppdragXml))
        val message = jmsSession.createTextMessage(oppdragXml)
        message.jmsReplyTo = MQQueue(replyTo)
        producer.send(message)
    }

    /*private fun createMessage(message: String): MessageCreator {
        return MessageCreator {
            it.createTextMessage(message).apply {
                jmsReplyTo = it.createQueue(replyTo)
            }
        }
    }*/
    fun close() {
        log.info("Closing OppdragMQSender::producer")
        producer.close()
        log.info("Closing OppdragMQSender::jmsSession")
        jmsSession.close() // Hmmm.... men jmsSession er ikke thread-safe?
    }
}