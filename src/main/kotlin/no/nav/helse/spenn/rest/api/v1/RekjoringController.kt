package no.nav.helse.spenn.rest.api.v1

import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.spenn.oppdrag.OppdragStateDTO
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
//import no.nav.helse.spenn.oppdrag.fromFagId
//import no.nav.security.oidc.api.Protected
import org.slf4j.LoggerFactory
import java.util.*

/*@RestController
@Protected
@RequestMapping("/api/v1/rekjoring")*/
@KtorExperimentalAPI
class RekjoringController(val oppdragStateService: OppdragStateService,
                          val audit: AuditSupport) {

    companion object {
        private val LOG = LoggerFactory.getLogger(RekjoringController::class.java)
    }
/*
    //@PutMapping
    fun resetStateForRerun(/*@RequestParam(required = false, defaultValue = "")*/ fagId: String,
                           /*@RequestParam(required = false, defaultValue = "")*/ soknadId: String): List<String> {
        LOG.info("running reset state for rerun for: ${fagId} ${soknadId}")
        audit.info("rekjører for  ${fagId} ${soknadId}")
        val fagIdList = if (fagId.isNotEmpty()) fagId.split(",").map { it.fromFagId() } else listOf()
        val soknadList = if (soknadId.isNotEmpty()) soknadId.split(",").map { UUID.fromString(it) } else listOf()
        val oppdragList = fagIdList.map { oppdragStateService.fetchOppdragState(it) }.union(soknadList.map{oppdragStateService.fetchOppdragState(it)})
        return oppdragList.filter {
            it.status == OppdragStateStatus.FEIL || it.status == OppdragStateStatus.SIMULERING_FEIL
        }.map {
            resetState(it)
        }
    }

    //@PutMapping("/all")
    fun resetAllErrorStateForRerun(/*@RequestParam(required = false, defaultValue = "false")*/ includeSimulering: Boolean): List<String> {
        LOG.info("running reset all oppdragstate that got FEIL")
        audit.info("rekjører for alle oppdrag som har feil (og simulerfeil)")
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
*/
}