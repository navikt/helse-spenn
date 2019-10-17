package no.nav.helse.spenn.rest

import io.ktor.application.Application
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.streams.KafkaStreams

data class SpennApiEnvironment(
        val port: Int = 8080,
        val kafkaStreams: KafkaStreams,
        val meterRegistry: MeterRegistry
)

fun spennApiServer(env : SpennApiEnvironment) : ApplicationEngine =
    embeddedServer(factory = Netty, port = env.port, module = { spennApiModule(env) })

internal fun Application.spennApiModule(env: SpennApiEnvironment) {

    routing {

        healthstatuscontroller(env.kafkaStreams, env.meterRegistry)

    }

}