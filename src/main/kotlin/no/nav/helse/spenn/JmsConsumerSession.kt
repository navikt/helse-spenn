package no.nav.helse.spenn

import org.slf4j.Logger
import javax.jms.Connection
import javax.jms.Session

class JmsConsumerSession(val connection: Connection, val mottakQueue: String) {
    private val jmsSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
    private val consumer = jmsSession.createConsumer(jmsSession.createQueue(mottakQueue))


    fun setMessageListener(log: Logger, listener: (String) -> Unit) {
        consumer.setMessageListener { message ->
            try {
                val body = message.getBody(String::class.java)
                listener(body)
            } catch (err: Exception) {
                log.error("Klarte ikke å hente ut meldingsinnholdet: ${err.message}", err)
            }
        }
    }
}