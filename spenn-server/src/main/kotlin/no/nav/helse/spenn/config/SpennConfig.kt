package no.nav.helse.spenn.config

import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI

class SpennConfig(
    val schedulerEnabled: Boolean = false,
    val taskSimuleringEnabled: Boolean = false,
    val taskOppdragEnabled: Boolean = false,
    val taskAvstemmingEnabled: Boolean = false,

    val simuleringServiceUrl: String,
    val stsUrl: String,
    val stsRestUrl: String,

    val aktorRegisteretBaseUrl: String,

    val serviceUserUsername: String,
    val serviceUserPassword: String
) {
    @KtorExperimentalAPI
    companion object {
        fun from(cfg: ApplicationConfig): SpennConfig {
            val getBool = fun(key: String): Boolean {
                val prop = cfg.propertyOrNull(key)
                if (prop == null) return false else return prop.getString().equals("true")
            }
            return SpennConfig(
                schedulerEnabled = getBool("scheduler.enabled"),
                taskSimuleringEnabled = getBool("scheduler.tasks.simulering"),
                taskOppdragEnabled = getBool("scheduler.tasks.oppdrag"),
                taskAvstemmingEnabled = getBool("scheduler.tasks.avstemming"),
                simuleringServiceUrl = cfg.property("simulering.service.url").getString(),
                stsUrl = cfg.property("sts.soap.url").getString(),
                stsRestUrl = "http://security-token-service.default.svc.nais.local",
                aktorRegisteretBaseUrl = cfg.property("aktorregisteret.base-url").getString(),
                serviceUserUsername = cfg.property("serviceuser.username").getString(),
                serviceUserPassword = cfg.property("serviceuser.password").getString()
            )
        }
    }
}

