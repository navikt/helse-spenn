package no.nav.helse.spenn.rest.api.v1

import no.nav.helse.spenn.dao.OppdragStateService
import no.nav.helse.spenn.oppdrag.OppdragStateDTO
import no.nav.security.oidc.api.ProtectedWithClaims
import no.nav.security.oidc.context.OIDCRequestContextHolder
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

// NB: Sync tilgangsstyring med helse-spade, TODO: Bytt ut med gruppe-sjekk eller annet etter hvert
@ProtectedWithClaims(issuer = "ourissuer", claimMap = [
    "NAVident=S150563",
    "NAVident=T149391",
    "NAVident=E117646",
    "NAVident=S151395",
    "NAVident=H131243",
    "NAVident=T127350",
    "NAVident=S122648",
    "NAVident=G153965",
    "NAVident=R154509",
    "NAVident=E156407"
], combineWithOr = true)
@RestController
@RequestMapping("/api/v1")
class OppdragStateController(val oppdragStateService: OppdragStateService,
                             val oidcRequestContextHolder: OIDCRequestContextHolder) {

    companion object {
        private val LOG = LoggerFactory.getLogger(OppdragStateController::class.java)
        private val AUDIT_LOG = LoggerFactory.getLogger("auditLogger")
    }

    @GetMapping("/oppdrag/soknad/{soknadId}")
    fun getOppdragStateBySoknadId(@PathVariable soknadId: UUID): OppdragStateDTO {
        LOG.info("Rest retrieve for soknadId: ${soknadId}")
        AUDIT_LOG.info("Bruker=${currentNavIdent()} slår opp søknadId=${soknadId}")
        return oppdragStateService.fetchOppdragState(soknadId)
    }

    @GetMapping("/oppdrag/{id}")
    fun getOpppdragStateById(@PathVariable id: Long): OppdragStateDTO {
        LOG.info("Rest retrieve for id: ${id}")
        AUDIT_LOG.info("Bruker=${currentNavIdent()} slår opp oppdragId=${id}")
        return oppdragStateService.fetchOppdragStateById(id)
    }

    private fun currentNavIdent() = oidcRequestContextHolder.oidcValidationContext.getClaims("ourissuer").get("NAVident")

}