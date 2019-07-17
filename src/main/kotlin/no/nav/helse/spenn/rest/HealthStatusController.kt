package no.nav.helse.spenn.rest

import no.nav.helse.spenn.dao.OppdragStateService
import no.nav.helse.spenn.vedtak.UtbetalingService
import org.apache.kafka.streams.KafkaStreams
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class HealthStatusController(val streams: KafkaStreams, val oppdragStateService: OppdragStateService,
                             val utbetalingService: UtbetalingService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(HealthStatusController::class.java)
    }
    @GetMapping("/internal/isAlive")
    fun isAlive(): String {
        return "ALIVE"
    }

    @GetMapping("/internal/isReady")
    fun isReady(): ResponseEntity<String> {
        //return if (streams.state().isRunning)
            return ResponseEntity.ok("READY")
        //else
        //    ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body("Kafka state is not running")
    }

    @GetMapping("/internal/dependsOn")
    fun dependsOn(): String {
        // TODO add more later
        return "Kafka state ${streams.state().name}"
    }

    @GetMapping("/internal/simulering/{soknadId}")
    fun simulering(@PathVariable soknadId: UUID): ResponseEntity<String> {
        val oppdrag = oppdragStateService.fetchOppdragState(soknadId)
        try {
            val result = utbetalingService.runSimulering(oppdrag)
            return ResponseEntity.ok("Result of simulering ${result.simuleringResult?.status} med utbetalt beløp: ${result.simuleringResult?.mottaker?.totalBelop}" +
                    "feilmelding ${result.simuleringResult?.feilMelding}")
        }
        catch(e: Exception) {
            LOG.error("feil i simulering",e)
            return ResponseEntity.badRequest().body("bad request")
        }
    }
}
