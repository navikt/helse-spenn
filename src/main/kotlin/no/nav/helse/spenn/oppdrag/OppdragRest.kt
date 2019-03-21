package no.nav.helse.spenn.oppdrag

import no.nav.helse.spenn.jms.OppdragMQSender
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/oppdrag")
class OppdragRest(val mqSender: OppdragMQSender) {

    @GetMapping("/{melding}")
    fun sendOppdrag(@PathVariable melding: String) {
        mqSender.sendOppdrag(melding)
    }
}