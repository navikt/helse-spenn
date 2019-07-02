package no.nav.helse.spenn.oppdrag


import no.nav.helse.spenn.grensesnittavstemming.JAXBAvstemmingsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jms.core.JmsTemplate
import org.springframework.jms.core.MessageCreator
import org.springframework.stereotype.Component


@Component
class AvstemmingMQSender(val jmsTemplate: JmsTemplate,
                         @Value("\${avstemming.queue.send}") val sendqueue: String,
                         val jaxb : JAXBAvstemmingsdata) {

    private val log = LoggerFactory.getLogger(AvstemmingMQSender::class.java)

    fun sendAvstemmingsmelding(avstemmingsMelding: Avstemmingsdata) {
        val xmlMelding = jaxb.fromAvstemmingsdataToXml(avstemmingsMelding)
        log.debug("sending $xmlMelding")
        log.debug("QUEUE: $sendqueue")
        jmsTemplate.send(sendqueue, createMessage(xmlMelding))
    }

    private fun createMessage(message: String): MessageCreator {
        return MessageCreator {
            it.createTextMessage(message)
        }
    }
}