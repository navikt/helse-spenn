package no.nav.helse.spenn.rest

import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.simulering.SimuleringService
//import no.nav.security.oidc.api.Unprotected
import org.apache.kafka.streams.KafkaStreams
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.PostConstruct

//@RestController
//@Unprotected
class HealthStatusController(val streams: KafkaStreams, val oppdragStateService: OppdragStateService,
                             val simuleringService: SimuleringService, val meterRegistry: MeterRegistry ) {

    private var stateCount = 0
    private val kafkaState = AtomicInteger(0)

    companion object {
        private val LOG = LoggerFactory.getLogger(HealthStatusController::class.java)
    }

    @PostConstruct
    fun init() {
        meterRegistry.gauge("KAFKA_STATE", kafkaState)
    }

    /*

    @GetMapping("/internal/isAlive")
    fun isAlive(): ResponseEntity<String> {
        if (streams.state().isRunning) {
            stateCount = 0
        }
        else  {
            if (++stateCount > 60) {
                LOG.error("Kafka stream has not been running for a while")
                return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).body("Kafka has been down for a long time!")
            }
        }
        return ResponseEntity.ok("ALIVE")
    }

    @GetMapping("/internal/isReady")
    fun isReady(): ResponseEntity<String> {
        kafkaState.set(streams.state().ordinal)
        return ResponseEntity.ok("READY")
    }

    @GetMapping("/internal/dependsOn")
    fun dependsOn(): String {
        return "Kafka state: ${streams.state().name}, stateCount: ${stateCount}"
    }

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
