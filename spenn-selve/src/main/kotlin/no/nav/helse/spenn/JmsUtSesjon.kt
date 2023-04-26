package no.nav.helse.spenn

import com.ibm.mq.jms.MQQueue
import javax.jms.Connection

class JmsUtSesjon(connection: Connection, sendQueue: String, private val replyTo: String? = null) : UtKø {
    private val jmsSession = connection.createSession()
    private val producer = jmsSession.createProducer(jmsSession.createQueue(sendQueue))

    override fun send(messageString: String) {
        val message = jmsSession.createTextMessage(messageString)
        replyTo
            ?.let { MQQueue(it) }
            ?.let { message.jmsReplyTo = it }
        producer.send(message)
    }
}