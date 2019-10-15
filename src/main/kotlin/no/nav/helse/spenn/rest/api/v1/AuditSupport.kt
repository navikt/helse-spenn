package no.nav.helse.spenn.rest.api.v1



//import no.nav.security.oidc.context.OIDCRequestContextHolder
import org.slf4j.LoggerFactory
//import org.springframework.stereotype.Component


//@Component
class AuditSupport() { //val oidcRequestContextHolder: OIDCRequestContextHolder) {

    companion object {
        private val AUDIT_LOG = LoggerFactory.getLogger("auditLogger")
    }

    fun navIdent() = "1234" //oidcRequestContextHolder.oidcValidationContext.getClaims("ourissuer").get("NAVident")

    fun info(message: String) {
        AUDIT_LOG.info("Bruker=${navIdent()} ${message}")
    }

}
