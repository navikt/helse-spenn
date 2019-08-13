package no.nav.helse.spenn.rest.api.v1

import no.nav.helse.spenn.dao.OppdragStateService
import no.nav.helse.spenn.oppdrag.OppdragStateDTO
import no.nav.security.oidc.api.Protected
import no.nav.security.oidc.context.OIDCRequestContextHolder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

// NB: Sync tilgangsstyring med helse-spade
@Protected
@RestController
@RequestMapping("/api/v1")
class OppdragStateController(val oppdragStateService: OppdragStateService,
                             val oidcRequestContextHolder: OIDCRequestContextHolder,
                             @Value("\${api.access.requiredgroup:group1}") val requiredGroupMembership: String ) {

    companion object {
        private val LOG = LoggerFactory.getLogger(OppdragStateController::class.java)
        private val AUDIT_LOG = LoggerFactory.getLogger("auditLogger")
    }

    @GetMapping("/oppdrag/soknad/{soknadId}")
    fun getOppdragStateBySoknadId(@PathVariable soknadId: UUID): OppdragStateDTO {
        validateGroupMembership()
        LOG.info("Rest retrieve for soknadId: ${soknadId}")
        AUDIT_LOG.info("Bruker=${currentNavIdent()} slår opp søknadId=${soknadId}")
        return oppdragStateService.fetchOppdragState(soknadId)
    }

    @GetMapping("/oppdrag/{id}")
    fun getOpppdragStateById(@PathVariable id: Long): OppdragStateDTO {
        validateGroupMembership()
        LOG.info("Rest retrieve for id: ${id}")
        AUDIT_LOG.info("Bruker=${currentNavIdent()} slår opp oppdragId=${id}")
        return oppdragStateService.fetchOppdragStateById(id)
    }

    private fun validateGroupMembership() {
        if (currentUserGroups() == null || (!currentUserGroups().contains(requiredGroupMembership))) {
            LOG.debug("${currentNavIdent()} prøvde accessere API men mangler gruppen $requiredGroupMembership")
            throw MissingGroupException("Missing group $requiredGroupMembership in JWT")
        }
    }

    private fun currentNavIdent() = oidcRequestContextHolder.oidcValidationContext.getClaims("ourissuer").get("NAVident")
    private fun currentUserGroups() = oidcRequestContextHolder.oidcValidationContext.getClaims("ourissuer").getAsList("groups")
}

@ResponseStatus(HttpStatus.UNAUTHORIZED)
private class MissingGroupException(message: String) : RuntimeException(message)