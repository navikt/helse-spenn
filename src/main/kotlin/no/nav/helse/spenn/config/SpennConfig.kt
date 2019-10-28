package no.nav.helse.spenn.config

import io.ktor.config.ApplicationConfig

class SpennConfig(
        val schedulerEnabled: Boolean = false,
        val taskSimuleringEnabled: Boolean = false,
        val taskOppdragEnabled: Boolean = false,
        val taskAvstemmingEnabled: Boolean = false,

        val simuleringServiceUrl:String,
        val stsUrl:String,
        val stsUsername:String,
        val stsPassword:String,
        val stsRestUrl:String,
        val stsRestUsername:String,
        val stsRestPassword:String,

        val aktorRegisteretBaseUrl:String
) {
    companion object {
        fun from(cfg:ApplicationConfig) : SpennConfig {
            val getBool = fun(key : String) : Boolean {
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
                    stsUsername = cfg.property("sts.soap.username").getString(),
                    stsPassword = cfg.property("sts.soap.password").getString(),
                    stsRestUrl = cfg.property("sts.rest.url").getString(),
                    stsRestUsername = cfg.property("sts.rest.username").getString(),
                    stsRestPassword = cfg.property("sts.rest.password").getString(),
                    aktorRegisteretBaseUrl = cfg.property("aktorregisteret.base-url").getString()
            )
        }
    }
}

