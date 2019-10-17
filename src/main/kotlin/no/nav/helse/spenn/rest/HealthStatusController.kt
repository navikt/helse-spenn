package no.nav.helse.spenn.rest

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.streams.KafkaStreams
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

private val LOG = LoggerFactory.getLogger("healthstatuscontroller")

fun Route.healthstatuscontroller(streams: KafkaStreams,
                                 meterRegistry: MeterRegistry ) {

    var stateCount = 0
    val kafkaState = AtomicInteger(0)
    meterRegistry.gauge("KAFKA_STATE", kafkaState)

    get("/internal/isAlive") {
        if (streams.state().isRunning) {
            stateCount = 0
        }
        else  {
            if (++stateCount > 60) {
                LOG.error("Kafka stream has not been running for a while")
                call.respond(HttpStatusCode.FailedDependency, "Kafka has been down for a long time!")
                return@get
            }
        }
        call.respondText("ALIVE")
    }

    get("/internal/isReady") {
        kafkaState.set(streams.state().ordinal)
        call.respondText("READY")
    }

    get("/internal/dependsOn") {
        call.respondText("Kafka state: ${streams.state().name}, stateCount: ${stateCount}")
    }

/*  // Hvorfor er dette her? :
    @GetMapping("/internal/simulering/{soknadId}")
    fun simulering(@PathVariable soknadId: UUID): ResponseEntity<String> {
        val oppdrag = oppdragStateService.fetchOppdragState(soknadId)
        try {
            val result = simuleringService.runSimulering(oppdrag)
            return ResponseEntity.ok("Result of simulering ${result.simuleringResult?.status} med utbetalt beløp: ${result.simuleringResult?.mottaker?.totalBelop}" +
                    "feilmelding ${result.simuleringResult?.feilMelding}")
        }
        catch(e: Exception) {
            LOG.error("feil i simulering",e)
            return ResponseEntity.badRequest().body("bad request")
        }
    }
     */
}
