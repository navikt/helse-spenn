package no.nav.helse.spenn

import no.nav.helse.spenn.utbetaling.Kvitteringer
import org.slf4j.LoggerFactory
import javax.jms.Connection
import javax.jms.Session

class Jms(private val connection: Connection, private val sendQueue: String, private val replyTo: String? = null) : Kø {
    private val log = LoggerFactory.getLogger(Kvitteringer::class.java)

    //TODO: Sesjonsbegrepet blir brukt likt som originalen. Vurder en sesjon per inkommende pakke (i praksis en per send)
    override fun sendSession() = JmsUtSesjon(connection, sendQueue, replyTo)

    override fun setMessageListener(listener: (String) -> Unit) {
        val jmsSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
        val consumer = jmsSession.createConsumer(jmsSession.createQueue(replyTo))
        consumer.setMessageListener { message ->
            try {
                val body = message.getBody(String::class.java)
                listener(body)
            } catch (err: Exception) {
                //TODO: Feilhåndteringen er lik originalen, vurder å kaste videre
                log.error("Klarte ikke å hente ut meldingsinnholdet for kø $replyTo: ${err.message}", err)
            }
        }
    }

    override fun start() {
        connection.start()
    }

    override fun close() {
        connection.stop()
    }
}