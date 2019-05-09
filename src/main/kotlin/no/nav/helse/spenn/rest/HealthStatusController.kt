package no.nav.helse.spenn.rest

import org.apache.kafka.streams.KafkaStreams
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthStatusController(val streams: KafkaStreams) {

    @GetMapping("/internal/isAlive")
    fun isAlive(): String {
        return "ALIVE"
    }

    @GetMapping("/internal/isReady")
    fun isReady(): ResponseEntity<String> {
        return ResponseEntity.ok("isReady")
//        if (streams.state().isRunning)
//            return ResponseEntity.ok("READY")
//        else
//            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body("Kafka is not running")
    }

    @GetMapping("/internal/dependsOn")
    fun dependsOn(): String {
        // TODO add more later
        return "Kafka state ${streams.state().name}"
    }

}
