package no.nav.helse.spenn.simulering.api

import io.ktor.server.auth.jwt.*
import io.ktor.server.auth.jwt.jwt

class AzureApp(
    private val jwkProvider: com.auth0.jwk.JwkProvider,
    private val issuer: String,
    private val clientId: String,
) {
    fun konfigurerJwtAuth(config: io.ktor.server.auth.AuthenticationConfig) {
        config.jwt {
            verifier(jwkProvider, issuer) {
                withAudience(clientId)
                withClaimPresence("azp_name")
            }
            validate { credentials ->
                JWTPrincipal(credentials.payload)
            }
        }
    }
}