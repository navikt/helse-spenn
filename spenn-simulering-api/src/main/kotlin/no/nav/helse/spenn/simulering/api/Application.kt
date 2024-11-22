package no.nav.helse.spenn.simulering.api

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.azure.createAzureTokenClientFromEnvironment
import com.github.navikt.tbd_libs.naisful.naisApp
import com.github.navikt.tbd_libs.result_object.map
import com.github.navikt.tbd_libs.result_object.ok
import com.github.navikt.tbd_libs.soap.InMemoryStsClient
import com.github.navikt.tbd_libs.soap.MinimalSoapClient
import com.github.navikt.tbd_libs.soap.MinimalStsClient
import com.github.navikt.tbd_libs.soap.samlStrategy
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.request.header
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import no.nav.helse.spenn.simulering.api.client.SimuleringV2Service
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient

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
        azureClient.bearerToken(env.getValue("WS_PROXY_SCOPE")).map {
            "Bearer ${it.token}".ok()
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

    val app = naisApp(
        meterRegistry = meterRegistry,
        objectMapper = objectmapper,
        applicationLogger = logg,
        callLogger = LoggerFactory.getLogger("no.nav.helse.spenn.simulering.api.CallLogging"),
        timersConfig = { call, _ ->
            this
                .tag("azp_name", call.principal<JWTPrincipal>()?.get("azp_name") ?: "n/a")
                // https://github.com/linkerd/polixy/blob/main/DESIGN.md#l5d-client-id-client-id
                // eksempel: <APP>.<NAMESPACE>.serviceaccount.identity.linkerd.cluster.local
                .tag("konsument", call.request.header("L5d-Client-Id") ?: "n/a")
        },
        mdcEntries = mapOf(
            "azp_name" to { call: ApplicationCall -> call.principal<JWTPrincipal>()?.get("azp_name") },
            "konsument" to { call: ApplicationCall -> call.request.header("L5d-Client-Id") }
        ),
    ) {
        authentication { azureApp.konfigurerJwtAuth(this) }
        lagApplikasjonsmodul(simuleringtjeneste)
    }
    app.start(wait = true)
}

fun Application.lagApplikasjonsmodul(simuleringtjeneste: Simuleringtjeneste) {
    install(DoubleReceive)
    routing {
        authenticate {
            api(simuleringtjeneste)
        }
    }
}
