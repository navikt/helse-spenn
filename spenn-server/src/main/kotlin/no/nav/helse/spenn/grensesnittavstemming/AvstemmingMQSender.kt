package no.nav.helse.spenn.grensesnittavstemming

import no.nav.helse.spenn.oppdrag.JAXBAvstemmingsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import org.slf4j.LoggerFactory
import javax.jms.Connection

open class AvstemmingMQSender(
    connection: Connection,
    private val sendqueue: String,
    private val jaxb: JAXBAvstemmingsdata
) {

    private val jmsSession = connection.createSession()
    private val producer = jmsSession.createProducer(jmsSession.createQueue(sendqueue))

    private val log = LoggerFactory.getLogger(AvstemmingMQSender::class.java)

    open fun sendAvstemmingsmelding(avstemmingsMelding: Avstemmingsdata) {
        val xmlMelding = jaxb.fromAvstemmingsdataToXml(avstemmingsMelding)
        log.trace("sending $xmlMelding")
        log.trace("QUEUE: $sendqueue")
        producer.send(jmsSession.createTextMessage(xmlMelding))
    }
}
