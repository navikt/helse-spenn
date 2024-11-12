package no.nav.helse.spenn

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.azure.createAzureTokenClientFromEnvironment
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import com.github.navikt.tbd_libs.spenn.SimuleringClient
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.spenn.simulering.Simuleringer
import java.net.http.HttpClient

fun main() {
    val env = System.getenv()
    rapidApp(env)
}

private fun rapidApp(env: Map<String, String>) {
    val azureClient = createAzureTokenClientFromEnvironment(env)
    val httpClient = HttpClient.newHttpClient()
    val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    val simuleringClient = SimuleringClient(httpClient, objectMapper, azureClient)

    val rapid: RapidsConnection = RapidApplication.create(env)
    rapidApp(rapid, simuleringClient)
    rapid.start()
}

fun rapidApp(rapid: RapidsConnection, simuleringClient: SimuleringClient) {
    rapid.apply {
        Simuleringer(this, simuleringClient)
    }
}