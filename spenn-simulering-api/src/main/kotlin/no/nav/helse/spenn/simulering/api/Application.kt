package no.nav.helse.spenn.simulering.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.azure.AzureTokenProvider
import com.github.navikt.tbd_libs.azure.createAzureTokenClientFromEnvironment
import com.github.navikt.tbd_libs.soap.InMemoryStsClient
import com.github.navikt.tbd_libs.soap.MinimalSoapClient
import com.github.navikt.tbd_libs.soap.MinimalStsClient
import com.github.navikt.tbd_libs.soap.samlStrategy
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.application.serverConfig
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.uri
import io.ktor.server.response.ApplicationSendPipeline
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.util.toMap
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import net.logstash.logback.argument.StructuredArguments.keyValue
import net.logstash.logback.argument.StructuredArguments.v
import no.nav.helse.spenn.simulering.api.client.SimuleringV2Service
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.net.URI
import java.net.http.HttpClient
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import io.ktor.server.plugins.doublereceive.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry

private val logg = LoggerFactory.getLogger(::main.javaClass)
private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
private val objectmapper = jacksonObjectMapper()
    .registerModules(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e ->
        logg.error("Ufanget exception: {}", e.message, e)
        sikkerlogg.error("Ufanget exception: {}", e.message, e)
    }

    System.setProperty("io.ktor.development", (System.getenv("NAIS_CLUSTER_NAME") == "dev-gcp").toString())

    configureAndLaunchApp(System.getenv())
}

private fun configureAndLaunchApp(env: Map<String, String>) {
    val azureApp = AzureApp(
        jwkProvider = com.auth0.jwk.JwkProviderBuilder(URI(env.getValue("AZURE_OPENID_CONFIG_JWKS_URI")).toURL()).build(),
        issuer = env.getValue("AZURE_OPENID_CONFIG_ISSUER"),
        clientId = env.getValue("AZURE_APP_CLIENT_ID"),
    )

    val serviceAccountUserName = env.getValue("SERVICEUSER_NAME")
    val serviceAccountPassword = env.getValue("SERVICEUSER_PASSWORD")

    val azureClient = createAzureTokenClientFromEnvironment(env)
    val proxyAuthorization = {
        when (val result = azureClient.bearerToken(env.getValue("WS_PROXY_SCOPE"))) {
            is AzureTokenProvider.AzureTokenResult.Error -> throw RuntimeException("Fikk ikke azure token: ${result.error}", result.exception)
            is AzureTokenProvider.AzureTokenResult.Ok -> "Bearer ${result.azureToken.token}"
        }
    }

    val httpClient = HttpClient.newHttpClient()
    val simuleringClient = SimuleringV2Service(
        MinimalSoapClient(
            serviceUrl = URI(env.getValue("SIMULERING_SERVICE_URL")),
            tokenProvider = InMemoryStsClient(
                MinimalStsClient(
                    baseUrl = URI(env.getValue("GANDALF_BASE_URL")),
                    httpClient = httpClient,
                    proxyAuthorization = proxyAuthorization
                )
            ),
            httpClient = httpClient,
            proxyAuthorization = proxyAuthorization
        ),
        samlStrategy(serviceAccountUserName, serviceAccountPassword)
    )

    val simuleringtjeneste = Simuleringtjeneste(simuleringClient)

    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT, PrometheusRegistry.defaultRegistry, Clock.SYSTEM)
    val app = naisApp(meterRegistry, objectmapper) {
        authentication { azureApp.konfigurerJwtAuth(this) }
        lagApplikasjonsmodul(simuleringtjeneste)
    }
    app.start(wait = true)
}

fun naisApp(
    meterRegistry: PrometheusMeterRegistry,
    objectMapper: ObjectMapper,
    applicationLogger: Logger = logg,
    port: Int = 8080,
    applicationModule: Application.() -> Unit
): EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration> {
    val config = serverConfig(
        environment = applicationEnvironment {
            log = applicationLogger
        }
    ) {
        module { standardApiModule(meterRegistry, objectMapper) }
        module(applicationModule)
    }
    val app = EmbeddedServer(config, CIO) {
        connectors.add(EngineConnectorBuilder().apply {
            this.port = port
        })
    }
    return app
}

fun Application.standardApiModule(meterRegistry: PrometheusMeterRegistry, objectMapper: ObjectMapper) {
    val readyToggle = AtomicBoolean(false)
    monitor.subscribe(ApplicationStarted) {
        readyToggle.set(true)
    }
    install(CallId) {
        header("callId")
        verify { it.isNotEmpty() }
        generate { UUID.randomUUID().toString() }
    }
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }
    install(CallLogging) {
        logger = LoggerFactory.getLogger("no.nav.helse.spenn.simulering.api.CallLogging")
        level = Level.INFO
        callIdMdc("callId")
        disableDefaultColors()
        filter { call -> call.request.path().startsWith("/api/") }
    }
    nais(readyToggle, meterRegistry)
}

fun Application.lagApplikasjonsmodul(simuleringtjeneste: Simuleringtjeneste) {
    install(DoubleReceive)
    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, FeilResponse(
                feilmelding = "Ugyldig request: ${cause.message}\n${cause.stackTraceToString()}",
                callId = call.callId
            ))
        }
        exception<NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, FeilResponse(
                feilmelding = "Ikke funnet: ${cause.message}\n${cause.stackTraceToString()}",
                callId = call.callId
            ))
        }
        exception<Throwable> { call, cause ->
            call.application.log.info("ukjent feil: ${cause.message}. svarer med InternalServerError og en feilmelding i JSON", cause)
            call.respond(HttpStatusCode.InternalServerError, FeilResponse(
                feilmelding = "Tjeneren møtte på ein feilmelding: ${cause.message}\n${cause.stackTraceToString()}",
                callId = call.callId
            ))
        }
    }
    requestResponseTracing(LoggerFactory.getLogger("no.nav.helse.spenn.simulering.api.Tracing"))
    routing {
        authenticate {
            api(simuleringtjeneste)
        }
    }
}

data class FeilResponse(
    val feilmelding: String,
    val callId: String?
)

private const val isaliveEndpoint = "/isalive"
private const val isreadyEndpoint = "/isready"
private const val metricsEndpoint = "/metrics"

private val ignoredPaths = listOf(metricsEndpoint, isaliveEndpoint, isreadyEndpoint)

private fun Application.requestResponseTracing(logger: Logger) {
    intercept(ApplicationCallPipeline.Monitoring) {
        if (call.request.uri in ignoredPaths) return@intercept proceed()
        val headers = call.request.headers.toMap()
            .filterNot { (key, _) -> key.lowercase() in listOf("authorization") }
            .map { (key, values) ->
                keyValue("req_header_$key", values.joinToString(separator = ";"))
            }.toTypedArray()
        logger.info("{} {}", v("method", call.request.httpMethod.value), v("uri", call.request.uri), *headers)
        proceed()
    }

    sendPipeline.intercept(ApplicationSendPipeline.After) { message ->
        val status = call.response.status() ?: (when (message) {
            is OutgoingContent -> message.status
            is HttpStatusCode -> message
            else -> null
        } ?: HttpStatusCode.OK).also { status ->
            call.response.status(status)
        }

        if (call.request.uri in ignoredPaths) return@intercept
        logger.info("svarer status=${status.value} ${call.request.uri}")
    }
}

private fun Application.nais(readyToggle: AtomicBoolean, meterRegistry: PrometheusMeterRegistry) {
    install(MicrometerMetrics) {
        registry = meterRegistry
        meterBinders = listOf(
            ClassLoaderMetrics(),
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            ProcessorMetrics(),
            JvmThreadMetrics(),
        )
    }

    routing {
        get(isaliveEndpoint) {
            call.respondText("ALIVE", ContentType.Text.Plain)
        }

        get(isreadyEndpoint) {
            if (!readyToggle.get()) return@get call.respondText("NOT READY", ContentType.Text.Plain, HttpStatusCode.ServiceUnavailable)
            call.respondText("READY", ContentType.Text.Plain)
        }

        get(metricsEndpoint) {
            call.respond(meterRegistry.scrape())
        }
    }
}