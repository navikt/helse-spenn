package no.nav.helse.spenn.rest.api.v1

import no.nav.helse.spenn.oppdrag.OppdragStateDTO
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
import no.nav.security.oidc.api.Protected
import no.nav.security.oidc.api.Unprotected
import no.nav.security.oidc.context.OIDCRequestContextHolder
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@Protected
@RequestMapping("/api/v1/rekjoring")
class RekjoringController(val oppdragStateService: OppdragStateService,
                          val oidcRequestContextHolder: OIDCRequestContextHolder) {

    companion object {
        private val LOG = LoggerFactory.getLogger(RekjoringController::class.java)
    }

    @PutMapping
    fun resetStateForRerun(@RequestParam(required = false, defaultValue = "") id: String,
                           @RequestParam(required = false, defaultValue = "") soknadId: String): List<String> {
        LOG.info("running reset state for rerun for: ${id} ${soknadId}")
        val idList = if (id.isNotEmpty()) id.split(",").map { it.toLong() } else listOf()
        val soknadList = if (soknadId.isNotEmpty()) soknadId.split(",").map { UUID.fromString(it) } else listOf()
        val oppdragList = idList.map { oppdragStateService.fetchOppdragStateById(it) }.union(soknadList.map{oppdragStateService.fetchOppdragState(it)})
        return oppdragList.filter {
            it.status == OppdragStateStatus.FEIL || it.status == OppdragStateStatus.SIMULERING_FEIL
        }.map {
            resetState(it)
        }
    }

    @PutMapping("/all")
    fun resetAllErrorStateForRerun(@RequestParam(required = false, defaultValue = "false") includeSimulering: Boolean): List<String> {
        val feilList = if (includeSimulering) oppdragStateService.fetchOppdragStateByStatus(OppdragStateStatus.SIMULERING_FEIL)
                        .union(oppdragStateService.fetchOppdragStateByStatus(OppdragStateStatus.FEIL))
                      else oppdragStateService.fetchOppdragStateByStatus(OppdragStateStatus.FEIL)
        return feilList.map {
            resetState(it)
        }
    }

    private fun resetState(it: OppdragStateDTO): String {
        LOG.info("resetting oppdragstate for rerun: ${it.soknadId}")
        oppdragStateService.saveOppdragState(it.copy(status = OppdragStateStatus.STARTET))
        return it.soknadId.toString()
    }

}