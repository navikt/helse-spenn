package no.nav.helse.spenn.rest.api.v1

import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.simulering.SimuleringResult
import no.nav.security.oidc.api.Protected
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@Protected
@RequestMapping("/api/v1/simulering")
class SimuleringController(val oppdragStateService: OppdragStateService) {

    @GetMapping("/{soknadId}")
    fun getSimuleringBySoknadId(@PathVariable soknadId: UUID): SimuleringResult {
        return TODO()
    }

    @PostMapping("/{soknadId}")
    fun runSimulering(@PathVariable soknadId: UUID): SimuleringResult {
        return TODO()
    }
}