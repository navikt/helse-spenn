package no.nav.helse.spenn.rest.api.v1

import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService

import no.nav.helse.spenn.oppdrag.OppdragStateDTO
import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
import no.nav.helse.spenn.oppdrag.fromFagId
//import no.nav.security.oidc.api.Protected
import org.slf4j.LoggerFactory
//import org.springframework.web.bind.annotation.*
import java.util.*

// NB: Sync tilgangsstyring med helse-spade
//@Protected
//@RestController
//@RequestMapping("/api/v1/oppdrag")
@KtorExperimentalAPI
class OppdragStateController(val oppdragStateService: OppdragStateService,
                             val audit: AuditSupport) {

    /*companion object {
        private val LOG = LoggerFactory.getLogger(OppdragStateController::class.java)
    }

    //@GetMapping("/soknad/{soknadId}")
    fun getOppdragStateBySoknadId(//@PathVariable
                                  soknadId: UUID): OppdragStateDTO {
        LOG.info("Rest retrieve for soknadId: ${soknadId}")
        audit.info("slår opp soknadId=${soknadId}")
        return oppdragStateService.fetchOppdragState(soknadId)
    }

    //@GetMapping("/{id}")
    fun getOpppdragStateById(//@PathVariable
            id: Long): OppdragStateDTO {
        LOG.info("Rest retrieve for id: ${id}")
        audit.info("slår opp oppdragId=${id}")
        return oppdragStateService.fetchOppdragStateById(id)
    }

    //@GetMapping("/fagId/{fagId}")
    fun getOppdragStateByFagId(//@PathVariable
                               fagId: String): OppdragStateDTO {
        LOG.info("Rest retrive for fagId: ${fagId}")
        audit.info("slår opp fagId=${fagId}")
        return oppdragStateService.fetchOppdragState(fagId.fromFagId())
    }

    //@PutMapping("/{id}")
    fun updateOppdragState(//@PathVariable
                           id: Long, //@RequestBody
            dto: OppdragStateDTO): OppdragStateDTO {
        LOG.info("Rest update for id: ${id}")
        audit.info("oppdaterer oppdragId=${id}")
        return oppdragStateService.saveOppdragState(dto)
    }

    //@GetMapping("/status/{status}")
    fun getOppdragStateByStatus(//@PathVariable
                                status: OppdragStateStatus): List<OppdragStateDTO> {
        LOG.info("Rest retrieve for status: ${status}")
        audit.info("slår opp oppdrag på status=${status}")
        return oppdragStateService.fetchOppdragStateByStatus(status)
    }*/



}
