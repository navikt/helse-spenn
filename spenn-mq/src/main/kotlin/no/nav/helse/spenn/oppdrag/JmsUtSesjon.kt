package no.nav.helse.spenn.oppdrag

import com.ibm.mq.MQException
import com.ibm.mq.jms.MQQueue
import com.ibm.msg.client.wmq.common.internal.Reason
import org.slf4j.LoggerFactory
import javax.jms.Connection
import javax.jms.JMSException

class JmsUtSesjon(connection: Connection, sendQueue: String, private val replyTo: String? = null) : UtKø {
    private val jmsSession = connection.createSession()
    private val producer = jmsSession.createProducer(jmsSession.createQueue(sendQueue))
    private val log = LoggerFactory.getLogger(JmsUtSesjon::class.java)

    override fun send(messageString: String, priority: Int) {
        try {
            sendToMq(messageString, priority)
        } catch (err: JMSException) {
            val message = err.message
            val cause = err.cause

            if ((cause != null && cause is MQException && Reason.isConnectionBroken(cause.reason))
                || (message != null && message.contains("Failed to send a message to destination"))
            ) {
                throw MQErNede("Kan ikke sende melding på MQ-kø", err)
            }

            throw err
        }
    }

    private fun sendToMq(messageString: String, priority: Int) {

        val message = jmsSession.createTextMessage(messageString)
        replyTo
            ?.let { MQQueue(it) }
            ?.let { message.jmsReplyTo = it }

        log.info("""
            our calculated priority: $priority
            priority: ${producer.priority}
            deliveryMode: ${producer.deliveryMode}
            timeToLive: ${producer.timeToLive}
        """)

        producer.send(message)
    }
}