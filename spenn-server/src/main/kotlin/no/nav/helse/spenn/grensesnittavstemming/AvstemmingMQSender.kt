package no.nav.helse.spenn.oppdrag

import com.ibm.mq.jms.MQQueue
import com.ibm.msg.client.wmq.WMQConstants
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import org.slf4j.LoggerFactory
import javax.jms.Connection

open class AvstemmingMQSender(connection: Connection,
                              val sendqueue: String,
                              val jaxb: JAXBAvstemmingsdata
) {

    val jmsSession = connection.createSession()
    val producer = jmsSession
            .createProducer(MQQueue(sendqueue).apply {
                targetClient = WMQConstants.WMQ_CLIENT_NONJMS_MQ
            })
    private val log = LoggerFactory.getLogger(AvstemmingMQSender::class.java)

    open fun sendAvstemmingsmelding(avstemmingsMelding: Avstemmingsdata) {
        val xmlMelding = jaxb.fromAvstemmingsdataToXml(avstemmingsMelding)
        log.trace("sending $xmlMelding")
        log.trace("QUEUE: $sendqueue")
        producer.send(jmsSession.createTextMessage(xmlMelding))
    }

    fun close() {
        log.info("Closing AvstemmingMQSender::producer")
        producer.close()
        log.info("Closing AvstemmingMQSender::jmsSession")
        jmsSession.close() // Hmmm.... men jmsSession er ikke thread-safe?
    }
}
