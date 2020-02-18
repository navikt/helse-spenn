package no.nav.helse.spenn.rest

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.helse.spenn.AuthEnvironment
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import no.nav.helse.spenn.ourIssuer
import no.nav.helse.spenn.overforing.OppdragMQSender
import no.nav.helse.spenn.rest.api.v1.AuditSupport
import no.nav.helse.spenn.rest.api.v1.AuditSupport.Companion.identClaimForAuditLog
import no.nav.helse.spenn.rest.api.v1.opphørscontroller
import no.nav.helse.spenn.rest.api.v1.simuleringcontroller
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.vedtak.fnr.AktørTilFnrMapper
import no.nav.security.token.support.ktor.IssuerConfig
import no.nav.security.token.support.ktor.TokenSupportConfig
import no.nav.security.token.support.ktor.tokenValidationSupport
import org.apache.kafka.streams.KafkaStreams
import org.slf4j.LoggerFactory

@KtorExperimentalAPI
data class SpennApiEnvironment(
    val port: Int = 8080,
    val kafkaStreams: KafkaStreams,
    val meterRegistry: PrometheusMeterRegistry,
    val authConfig: AuthEnvironment,
    val simuleringService: SimuleringService,
    val aktørTilFnrMapper: AktørTilFnrMapper,
    val auditSupport: AuditSupport,
    val stateService: OppdragService,
    val oppdragMQSender: OppdragMQSender
)

@KtorExperimentalAPI
fun spennApiServer(env: SpennApiEnvironment): ApplicationEngine =
    embeddedServer(factory = Netty, port = env.port, module = { spennApiModule(env) })

@KtorExperimentalAPI
internal fun Application.spennApiModule(env: SpennApiEnvironment) {

    val log = LoggerFactory.getLogger("spennApiModule")

    install(Authentication) {
        tokenValidationSupport(config = TokenSupportConfig(
            IssuerConfig(
                name = ourIssuer,
                acceptedAudience = listOf(env.authConfig.acceptedAudience),
                discoveryUrl = env.authConfig.discoveryUrl.toString()
            )
        ),
            additionalValidation = {
                val claims = it.getClaims(ourIssuer)
                val groups = claims?.getAsList("groups")
                val hasGroup = groups != null && groups.contains(env.authConfig.requiredGroup)
                if (!hasGroup) log.info("missing required group ${env.authConfig.requiredGroup}")
                val hasIdentRequiredForAuditLog = claims?.getStringClaim(identClaimForAuditLog) != null
                if (!hasIdentRequiredForAuditLog) log.info("missing claim $identClaimForAuditLog required for auditlog")
                hasGroup && hasIdentRequiredForAuditLog
            })
    }

    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }

        install(MicrometerMetrics) {
            registry = env.meterRegistry
        }
    }

    routing {
        healthstatuscontroller(env.kafkaStreams, env.meterRegistry)

        authenticate {
            simuleringcontroller(env.simuleringService, env.aktørTilFnrMapper, env.auditSupport)
            opphørscontroller(env.stateService, env.aktørTilFnrMapper, env.auditSupport)
        }

    }
}
