package no.nav.helse.spenn.config

import io.ktor.config.ApplicationConfig

class SpennMQConfig(
        val mqEnabled: Boolean, // TODO
        val queueManager: String,
        val channel: String,
        val hostname: String,
        val port: Int,
        val user: String,
        val password: String,
        val oppdragQueueSend: String,
        val oppdragQueueMottak: String,
        val avstemmingQueueSend: String) {
    companion object {
        @io.ktor.util.KtorExperimentalAPI
        fun from(cfg: ApplicationConfig) : SpennMQConfig {
            val getBool = fun(key : String) : Boolean {
                val prop = cfg.propertyOrNull(key)
                if (prop == null) return false else return prop.getString().equals("true")
            }
            return SpennMQConfig(
                    mqEnabled = getBool("jms.mq.enabled"),
                    queueManager = cfg.property("ibm.mq.queue-manager").getString(),
                    channel = cfg.property("ibm.mq.channel").getString(),
                    hostname = cfg.property("ibm.mq.hostname").getString(),
                    port = cfg.property("ibm.mq.port").getString().toInt(),
                    user = cfg.property("ibm.mq.user").getString(),
                    password = cfg.property("ibm.mq.password").getString(),
                    oppdragQueueSend = cfg.property("oppdrag.queue.send").getString(),
                    oppdragQueueMottak = cfg.property("oppdrag.queue.mottak").getString(),
                    avstemmingQueueSend = cfg.property("avstemming.queue.send").getString()
            )
        }
    }
}