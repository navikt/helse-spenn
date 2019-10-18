package no.nav.helse.spenn.rest.api.v1



//import no.nav.security.oidc.context.OIDCRequestContextHolder
import io.ktor.application.call
import io.ktor.auth.AuthenticationContext
import io.ktor.auth.authentication
import no.nav.helse.spenn.rest.SpennApiAuthConfig
import no.nav.security.token.support.ktor.TokenValidationContextPrincipal
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException

//import org.springframework.stereotype.Component



//@Component
class AuditSupport(private val authConfig : SpennApiAuthConfig) { //val oidcRequestContextHolder: OIDCRequestContextHolder) {

    companion object {
        private val AUDIT_LOG = LoggerFactory.getLogger("auditLogger")
    }

    private fun navIdent(auth: AuthenticationContext) : String {
        val ident = auth.principal<TokenValidationContextPrincipal>()?.context
                ?.getClaims(authConfig.acceptedIssuer)
                ?.getStringClaim("NAVident")
        if (ident == null) {
            throw IllegalStateException("Using no token or token without required claim in auditlogging")
        }
        return ident
    }

    fun info(message: String, auth: AuthenticationContext) {
        AUDIT_LOG.info("Bruker=${navIdent(auth)} ${message}")
    }

}
