package no.nav.helse.spenn.rest.api.v1

import no.nav.security.oidc.context.OIDCRequestContextHolder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletResponse





@Component
class AllowUserCheckFilter(val oidcRequestContextHolder: OIDCRequestContextHolder,
                           @Value("\${api.access.requiredgroup:group1}") val requiredGroupMembership: String): Filter {

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        if (ourIssuer() == null || currentUserGroups() == null || (!currentUserGroups().contains(requiredGroupMembership))) {
            (response as HttpServletResponse).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing group $requiredGroupMembership in JWT")
        }
        else {
            chain.doFilter(request,response)
        }

    }

    private fun ourIssuer () = oidcRequestContextHolder.oidcValidationContext.getClaims("ourissuer")
    private fun currentUserGroups() = ourIssuer().getAsList("groups")

}
