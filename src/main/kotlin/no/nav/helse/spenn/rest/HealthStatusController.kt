package no.nav.helse.spenn.rest

import org.apache.kafka.streams.KafkaStreams
import org.springframework.boot.actuate.jms.JmsHealthIndicator
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthStatusController(val kafkaStreams: KafkaStreams) {

    @GetMapping("/internal/isAlive")
    fun isAlive(): String {
        return "ALIVE"
    }

    @GetMapping("/internal/isReady")
    fun isReady(): String {
        return "READY"
    }

    @GetMapping("/internal/dependsOn")
    fun dependsOn(): String {
        // TODO add more later
        return "Kafka state ${kafkaStreams.state().name}"
    }

}
