package no.nav.helse.spenn

import com.github.navikt.tbd_libs.azure.createAzureTokenClientFromEnvironment
import com.github.navikt.tbd_libs.soap.InMemoryStsClient
import com.github.navikt.tbd_libs.soap.MinimalSoapClient
import com.github.navikt.tbd_libs.soap.MinimalStsClient
import com.github.navikt.tbd_libs.soap.samlStrategy
import io.ktor.server.application.Application
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidApplication.Builder
import no.nav.helse.rapids_rivers.RapidApplication.RapidApplicationConfig
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spenn.simulering.SimuleringV2Service
import no.nav.helse.spenn.simulering.Simuleringer
import no.nav.helse.spenn.simulering.api.Simuleringtjeneste
import java.net.URI
import java.net.http.HttpClient

fun main() {
    val env = System.getenv()
    rapidApp(env)
}

private fun rapidApp(env: Map<String, String>) {
    val serviceAccountUserName = env.getValue("SERVICEUSER_NAME")
    val serviceAccountPassword = env.getValue("SERVICEUSER_PASSWORD")

    val azureClient = createAzureTokenClientFromEnvironment(env)
    val proxyAuthorization = {
        "Bearer ${azureClient.bearerToken(env.getValue("WS_PROXY_SCOPE")).token}"
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

    val rapid: RapidsConnection = RapidApplication.create(env)
    rapidApp(rapid, simuleringtjeneste)
    rapid.start()
}

fun rapidApp(rapid: RapidsConnection, simuleringtjeneste: Simuleringtjeneste) {
    rapid.apply {
        Simuleringer(this, simuleringtjeneste)
    }
}