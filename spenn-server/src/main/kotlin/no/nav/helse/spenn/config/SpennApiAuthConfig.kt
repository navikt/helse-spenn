package no.nav.helse.spenn.config

import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import java.net.URL

data class SpennApiAuthConfig(
        val acceptedAudience: String,
        val discoveryUrl: URL,
        val requiredGroup: String)
{
    @KtorExperimentalAPI
    companion object {
        const val ourIssuer = "ourissuer"

        fun from(cfg: ApplicationConfig) : SpennApiAuthConfig {
            return SpennApiAuthConfig(
                    acceptedAudience = cfg.property("spenn-api.auth.oidc.accepted-audience").getString(),
                    discoveryUrl = URL(cfg.property("spenn-api.auth.oidc.discovery-url").getString()),
                    requiredGroup = cfg.property("spenn-api.auth.required-group").getString()
            )
        }
    }
}