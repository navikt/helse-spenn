package no.nav.helse.spenn

import com.github.navikt.tbd_libs.soap.InMemoryStsClient
import com.github.navikt.tbd_libs.soap.MinimalSoapClient
import com.github.navikt.tbd_libs.soap.MinimalStsClient
import com.github.navikt.tbd_libs.soap.samlStrategy
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spenn.simulering.SimuleringV2Service
import no.nav.helse.spenn.simulering.Simuleringer
import java.io.File
import java.net.URI
import java.net.http.HttpClient

fun main() {
    val env = System.getenv()
    rapidApp(env)
}

private fun rapidApp(env: Map<String, String>) {
    val serviceAccountUserName = env["SERVICEUSER_NAME"] ?: "/var/run/secrets/nais.io/service_user/username".readFile()
    val serviceAccountPassword = env["SERVICEUSER_PASSWORD"] ?: "/var/run/secrets/nais.io/service_user/password".readFile()

    val httpClient = HttpClient.newHttpClient()
    val simuleringClient = SimuleringV2Service(
        MinimalSoapClient(
            serviceUrl = URI(env.getValue("SIMULERING_SERVICE_URL")),
            tokenProvider = InMemoryStsClient(
                MinimalStsClient(
                    baseUrl = URI(env.getValue("GANDALF_BASE_URL")),
                    httpClient = httpClient
                )
            ),
            httpClient = httpClient
        ),
        samlStrategy(serviceAccountUserName, serviceAccountPassword)
    )

    val rapid: RapidsConnection = RapidApplication.create(env)
    rapidApp(rapid, simuleringClient)
    rapid.start()
}

fun rapidApp(rapid: RapidsConnection, simuleringV2Service: SimuleringV2Service) {
    rapid.apply {
        Simuleringer(this, simuleringV2Service)
    }
}

fun String.readFile() = File(this).readText(Charsets.UTF_8)
