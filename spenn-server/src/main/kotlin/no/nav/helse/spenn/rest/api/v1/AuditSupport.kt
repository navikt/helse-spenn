package no.nav.helse.spenn.rest.api.v1

import io.ktor.auth.AuthenticationContext
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.spenn.ourIssuer
import no.nav.security.token.support.ktor.TokenValidationContextPrincipal
import org.slf4j.LoggerFactory

@KtorExperimentalAPI
class AuditSupport {

    companion object {
        private val AUDIT_LOG = LoggerFactory.getLogger("auditLogger")

        val identClaimForAuditLog = "preferred_username"

    }

    private fun navIdent(auth: AuthenticationContext) : String {
        val ident = auth.principal<TokenValidationContextPrincipal>()?.context
                ?.getClaims(ourIssuer)
                ?.getStringClaim(identClaimForAuditLog)
        return ident ?:
        throw IllegalStateException("Using no token or token without required claim in auditlogging")
    }

    fun info(message: String, auth: AuthenticationContext) {
        AUDIT_LOG.info("Bruker=${navIdent(auth)} ${message}")
    }

}
