package no.nav.helse.spenn.rest.api.v1

import io.ktor.auth.AuthenticationContext
import no.nav.helse.spenn.config.SpennApiAuthConfig
import no.nav.security.token.support.ktor.TokenValidationContextPrincipal
import org.slf4j.LoggerFactory

class AuditSupport {

    companion object {
        private val AUDIT_LOG = LoggerFactory.getLogger("auditLogger")
    }

    private fun navIdent(auth: AuthenticationContext) : String {
        val ident = auth.principal<TokenValidationContextPrincipal>()?.context
                ?.getClaims(SpennApiAuthConfig.ourIssuer)
                ?.getStringClaim("NAVident")
        return ident ?:
        throw IllegalStateException("Using no token or token without required claim in auditlogging")
    }

    fun info(message: String, auth: AuthenticationContext) {
        AUDIT_LOG.info("Bruker=${navIdent(auth)} ${message}")
    }

}
