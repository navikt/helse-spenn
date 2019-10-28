package no.nav.helse.spenn.config

import io.ktor.config.ApplicationConfig

data class SpennKafkaConfig(
        val bootstrapServersUrl: String,
        val appId: String = "spenn-1",
        val kafkaUsername: String,
        val kafkaPassword: String,
        val navTruststorePath: String?,
        val navTruststorePassword: String?,
        val plainTextKafka: Boolean = false,
        val offsetReset: Boolean = false,
        val timeStampMillis: Long = -1,
        val streamVedtak: Boolean = true) {
    companion object {
        @io.ktor.util.KtorExperimentalAPI
        fun from(cfg: ApplicationConfig) : SpennKafkaConfig {
            val getBool = fun(key : String) : Boolean {
                val prop = cfg.propertyOrNull(key)
                if (prop == null) return false else return prop.getString().equals("true")
            }
            return SpennKafkaConfig(
                    bootstrapServersUrl = cfg.property("kafka.bootstrapservers").getString(),
                    appId = cfg.property("kafka.appid").getString(),
                    kafkaUsername = cfg.property("kafka.username").getString(),
                    kafkaPassword = cfg.property("kafka.password").getString(),
                    navTruststorePath = cfg.property("kafka.truststorepath").getString(),
                    navTruststorePassword = cfg.property("kafka.truststorepassword").getString(),
                    plainTextKafka = getBool("kafka.plaintext"),
                    offsetReset = getBool("kafka.offsetreset.enabled"),
                    timeStampMillis = cfg.property("kafka.offsetreset.timestamp-millis").getString().toLong(),
                    streamVedtak = getBool("kafka.streamvedtakenabled")
            )
        }
    }
}