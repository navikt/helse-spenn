package no.nav.helse.spenn.oppdrag

import no.nav.helse.spenn.defaultObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

data class OppdragFerdigInfo(val soknadId:String)

@Component
class OppdragStateKafkaProducer(val kafkaTemplate : KafkaTemplate<String, String>,
                                @Value("\${oppdragstate.topic.send}") val topic:String) {

    private val log = LoggerFactory.getLogger(OppdragStateKafkaProducer::class.java)

    fun send(info : OppdragFerdigInfo) {
        val message = defaultObjectMapper.writeValueAsString(info)
        log.debug("sending message='{}' to topic='{}'", message, topic)
        kafkaTemplate.send(topic, info.soknadId, message)
    }
}
