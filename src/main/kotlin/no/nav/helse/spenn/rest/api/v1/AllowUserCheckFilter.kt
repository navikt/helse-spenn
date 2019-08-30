package no.nav.helse.spenn.rest.api.v1

import no.nav.security.oidc.context.OIDCRequestContextHolder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Component
class AllowUserCheckFilter(val oidcRequestContextHolder: OIDCRequestContextHolder,
                           @Value("\${api.access.requiredgroup:group1}") val requiredGroupMembership: String): OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {

        if (ourIssuer() == null || currentUserGroups() == null || (!currentUserGroups().contains(requiredGroupMembership))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing group $requiredGroupMembership in JWT")
        }
        else {
            filterChain.doFilter(request,response)
        }
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI.substring(request.contextPath.length)
        return !path.startsWith("/api/")
    }

    private fun ourIssuer () = oidcRequestContextHolder.oidcValidationContext.getClaims("ourissuer")
    private fun currentUserGroups() = ourIssuer().getAsList("groups")

}
