package no.nav.helse.spenn.avstemming

import javax.jms.Connection

class JmsUtSesjon(connection: Connection, sendQueue: String) : UtKø {
    private val jmsSession = connection.createSession()
    private val producer = jmsSession.createProducer(jmsSession.createQueue(sendQueue))

    override fun send(messageString: String) {
        producer.send(jmsSession.createTextMessage(messageString))
    }
}