package no.nav.helse.spenn.oppdrag

import com.ibm.mq.MQException
import com.ibm.mq.jms.MQQueue
import com.ibm.msg.client.wmq.common.internal.Reason
import javax.jms.Connection
import javax.jms.JMSException

class JmsUtSesjon(connection: Connection, sendQueue: String, private val replyTo: String? = null) : UtKø {
    private val jmsSession = connection.createSession()
    private val producer = jmsSession.createProducer(jmsSession.createQueue(sendQueue))

    override fun send(messageString: String) {
        try {
            sendToMq(messageString)
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

    private fun sendToMq(messageString: String) {

        val message = jmsSession.createTextMessage(messageString)
        replyTo
            ?.let { MQQueue(it) }
            ?.let { message.jmsReplyTo = it }
        producer.send(message)
    }
}