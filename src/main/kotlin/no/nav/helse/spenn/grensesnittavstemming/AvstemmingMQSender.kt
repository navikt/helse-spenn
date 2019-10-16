package no.nav.helse.spenn.oppdrag


import com.ibm.mq.jms.MQQueue
import com.ibm.msg.client.wmq.WMQConstants
import no.nav.helse.spenn.grensesnittavstemming.JAXBAvstemmingsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import org.slf4j.LoggerFactory
/*import org.springframework.beans.factory.annotation.Value
import org.springframework.jms.core.JmsTemplate
import org.springframework.stereotype.Component*/


//@Component
open class AvstemmingMQSender(//val jmsTemplate: JmsTemplate,
                         /*@Value("\${avstemming.queue.send}")*/ val sendqueue: String,
                         val jaxb : JAXBAvstemmingsdata) {

    private val log = LoggerFactory.getLogger(AvstemmingMQSender::class.java)

    open fun sendAvstemmingsmelding(avstemmingsMelding: Avstemmingsdata) {
        val xmlMelding = jaxb.fromAvstemmingsdataToXml(avstemmingsMelding)
        log.debug("sending $xmlMelding")
        log.debug("QUEUE: $sendqueue")
        val queue = MQQueue(sendqueue).apply {
            targetClient = WMQConstants.WMQ_CLIENT_NONJMS_MQ
        }
        //jmsTemplate.convertAndSend(queue, xmlMelding)
    }
}