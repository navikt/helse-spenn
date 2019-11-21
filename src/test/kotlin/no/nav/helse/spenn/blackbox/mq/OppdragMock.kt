package no.nav.helse.spenn.blackbox.mq

import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import no.nav.helse.spenn.KvitteringAlvorlighetsgrad
import no.nav.helse.spenn.oppdrag.JAXBOppdrag
import no.trygdeetaten.skjema.oppdrag.Mmel
import org.slf4j.LoggerFactory

val jaxb = JAXBOppdrag()

private val log = LoggerFactory.getLogger(OppdragMock::class.java)

class OppdragMock(host: String,
                  port: Int,
                  channel: String,
                  queueManager: String,
                  oppdragQueue: String,
                  user: String,
                  password: String) {

    val connection = MQConnectionFactory().also {
            it.hostName = host
            it.port = port
            it.channel = channel
            it.queueManager = queueManager
            it.transportType = WMQConstants.WMQ_CM_CLIENT
        }.createConnection(user, password) //.also { it.start() }

    private val session = connection.createSession()
    private val consumer = session
        .createConsumer(session.createQueue(oppdragQueue))

    fun listen() {
        log.info("Setting up listener")
        consumer.setMessageListener {
            val message = it.getBody(String::class.java)
            log.info("Got message: $message")

            val oppdrag = jaxb.toOppdrag(message)

            oppdrag.mmel = Mmel().apply {
                this.alvorlighetsgrad = KvitteringAlvorlighetsgrad.OK.kode
                this.beskrMelding = "todo bien"
            }

            log.info("Creating producer for reply-queue: ${it.jmsReplyTo}")
            session.createProducer(it.jmsReplyTo).use { producer ->
                val resp = jaxb.fromOppdragToXml(oppdrag)

                val replaced = resp.replace("ns2:oppdrag xmlns:ns2", "oppdrag xmlns")

                log.info("Sending response $replaced")
                producer.send(session.createTextMessage(replaced))
                log.info("Sent response")
            }
        }
        log.info("Starting connection...")
        connection.start()
    }

}

