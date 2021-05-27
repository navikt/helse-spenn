package no.nav.helse.spenn

import com.ibm.mq.MQException
import com.ibm.mq.jms.MQQueue
import com.ibm.msg.client.wmq.common.internal.Reason
import no.nav.helse.spenn.utbetaling.MQErNede
import javax.jms.Connection
import javax.jms.JMSException

class JmsPublisherSession(val connection: Connection, sendQueue: String, val replyTo: String? = null) {
        val jmsSession = connection.createSession()
        val producer = jmsSession.createProducer(jmsSession.createQueue(sendQueue))

    fun send(messageString: String) {
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
            ?.let {MQQueue(it)}
            ?.let { message.jmsReplyTo = it }
        producer.send(message)
    }

    fun sendNoErrorHandling(messageString: String) {
        sendToMq(messageString)
    }
}